package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;
import com.aidemo.rag.dto.RagSource;
import com.aidemo.rag.dto.RagTraceStep;
import com.aidemo.rag.model.EmbeddingVector;
import com.aidemo.rag.model.KnowledgeChunk;
import com.aidemo.rag.model.KnowledgeDocument;
import com.aidemo.rag.repository.KnowledgeChunkRepository;
import com.aidemo.rag.repository.KnowledgeDocumentRepository;
import com.aidemo.rag.vector.VectorSearchQuery;
import com.aidemo.rag.vector.VectorSearchResult;
import com.aidemo.rag.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 默认 RAG 检索服务。
 *
 * <p>当前实现混合检索：问题向量化 -> 本地 VectorStore 初筛 -> 关键词候选补充 ->
 * 可解释 rerank -> 回填正文和来源。它仍然不调用大模型生成答案。</p>
 */
@Service
@RequiredArgsConstructor
public class DefaultRetrievalService implements RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KeywordScoringService keywordScoringService;
    private final RerankService rerankService;
    private final RagProperties ragProperties;

    @Override
    public RagSearchResponse search(RagSearchRequest request) {
        long startedAt = System.currentTimeMillis();
        int topK = request.getTopK() == null ? ragProperties.getTopK() : request.getTopK();
        double minScore = request.getMinScore() == null ? ragProperties.getMinScore() : request.getMinScore();

        EmbeddingVector queryVector = embeddingService.embed(request.getQuery());
        VectorSearchQuery vectorQuery = new VectorSearchQuery();
        vectorQuery.setVector(queryVector.getValues());
        vectorQuery.setTopK(Math.max(topK * 3, topK));
        vectorQuery.setMinScore(0.0d);
        vectorQuery.setTags(request.getTags());

        List<VectorSearchResult> vectorResults = vectorStore.search(vectorQuery);
        List<RagChunkHit> candidates = mergeCandidates(request, vectorResults);
        List<RagChunkHit> rerankedHits = rerankService.rerank(request.getQuery(), candidates, topK)
                .stream()
                .filter(hit -> hit.getRerankScore() >= minScore)
                .toList();

        RagSearchResponse response = new RagSearchResponse();
        response.setTraceId("rag-search-" + UUID.randomUUID());
        response.setOriginalQuery(request.getQuery());
        response.setRewrittenQuery(request.getQuery());
        response.setHits(rerankedHits);
        response.getSteps().add(step("query_embedding", "success", "问题已向量化，模型: " + embeddingService.modelName(), startedAt));
        response.getSteps().add(step("vector_search", "success", "向量初筛 " + vectorResults.size() + " 个 chunk", startedAt));
        response.getSteps().add(step("keyword_candidates", "success", "合并后候选 " + candidates.size() + " 个 chunk", startedAt));
        response.getSteps().add(step("explainable_rerank", "success", "重排后返回 " + rerankedHits.size() + " 个 chunk", startedAt));
        return response;
    }

    /**
     * 合并向量候选和关键词候选。
     *
     * <p>向量召回擅长语义相似，关键词候选擅长精确术语。这里用 chunkId 去重，
     * 让后续 rerank 在同一批候选上统一打分。</p>
     */
    private List<RagChunkHit> mergeCandidates(RagSearchRequest request, List<VectorSearchResult> vectorResults) {
        //先把向量搜索结果转换成候选 hit，并按 chunkId 放入候选池，后续会继续补关键词分和重排分
        Map<String, RagChunkHit> candidates = new LinkedHashMap<>();
        for (VectorSearchResult result : vectorResults) {
            RagChunkHit hit = toHit(result, request.isIncludeContent());
            candidates.put(hit.getChunkId(), hit);
        }
        //再查询所有的检索块，将满足的检索块也放入候选map,然后对所有候选的检索块进行打分及Token相关信息的记录
        for (KnowledgeChunk chunk : chunkRepository.findAll()) {
            //如果传入的筛选参数里携带了标签筛选,那就只筛选满足该标签的检索块(有的检索块可能会打上tag，用于检索时缩小范围，例如：部门信息，时间信息等)
            if (!matchesTags(chunk, request.getTags())) {
                continue;
            }
            //对检索块和问题进行打分
            KeywordScore keywordScore = keywordScoringService.score(request.getQuery(), chunk);
            if (keywordScore.getScore() <= 0.0d && !candidates.containsKey(chunk.getId())) {
                continue;
            }

            RagChunkHit hit = candidates.computeIfAbsent(chunk.getId(),
                    chunkId -> toHit(chunkId, 0.0d, request.isIncludeContent()));
            hit.setKeywordScore(keywordScore.getScore());
            hit.getMetadata().put("matchedTokenCount", keywordScore.getMatchedTokenCount());
            hit.getMetadata().put("matchedTokens", keywordScore.getMatchedTokens());
        }

        return candidates.values().stream()
                .peek(hit -> {
                    if (hit.getKeywordScore() == null) {
                        KeywordScore keywordScore = keywordScoringService.score(request.getQuery(),
                                chunkRepository.findById(hit.getChunkId()).orElseThrow());
                        hit.setKeywordScore(keywordScore.getScore());
                        hit.getMetadata().put("matchedTokenCount", keywordScore.getMatchedTokenCount());
                        hit.getMetadata().put("matchedTokens", keywordScore.getMatchedTokens());
                    }
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private boolean matchesTags(KnowledgeChunk chunk, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        Object rawTags = chunk.getMetadata().get("tags");
        if (!(rawTags instanceof List<?> storedTags)) {
            return false;
        }
        return storedTags.containsAll(tags);
    }

    private RagChunkHit toHit(String chunkId, double vectorScore, boolean includeContent) {
        return chunkRepository.findById(chunkId)
                .map(chunk -> toHitFromRepository(chunk, vectorScore, includeContent))
                .orElseThrow(() -> new IllegalStateException("索引指向了不存在的 chunk: " + chunkId));
    }

    /**
     * 把向量召回结果转换为候选 hit。
     *
     * <p>正常情况下从内存 chunk/document 仓储回填完整信息；如果后续启用 ES 持久化向量存储，
     * 应用重启后内存仓储可能为空，此时允许从 {@link VectorSearchResult} 中的持久化元数据兜底组装。</p>
     */
    private RagChunkHit toHit(VectorSearchResult result, boolean includeContent) {
        return chunkRepository.findById(result.getChunkId())
                .map(chunk -> toHitFromRepository(chunk, result.getScore(), includeContent))
                .orElseGet(() -> toHitFromVectorResult(result, includeContent));
    }

    private RagChunkHit toHitFromRepository(KnowledgeChunk chunk, double vectorScore, boolean includeContent) {
        KnowledgeDocument document = documentRepository.findById(chunk.getDocumentId())
                .orElseThrow(() -> new IllegalStateException("chunk 指向了不存在的文档: " + chunk.getDocumentId()));

        RagSource source = new RagSource();
        source.setDocumentId(document.getId());
        source.setChunkId(chunk.getId());
        source.setTitle(document.getTitle());
        source.setSourceUri(document.getSourceUri());
        source.setSectionTitle(chunk.getSectionTitle());
        source.setStartLine(chunk.getStartLine());
        source.setEndLine(chunk.getEndLine());
        source.setPageStart(chunk.getPageStart());
        source.setPageEnd(chunk.getPageEnd());
        source.setScore(vectorScore);

        RagChunkHit hit = new RagChunkHit();
        hit.setDocumentId(document.getId());
        hit.setChunkId(chunk.getId());
        hit.setTitle(document.getTitle());
        hit.setScore(vectorScore);
        hit.setVectorScore(vectorScore);
        hit.setKeywordScore(0.0d);
        hit.setRerankScore(vectorScore);
        hit.setSource(source);
        hit.setMetadata(new LinkedHashMap<>(chunk.getMetadata()));
        hit.setContentPreview(preview(chunk.getContent()));
        if (includeContent) {
            hit.setContent(chunk.getContent());
        }
        return hit;
    }

    private RagChunkHit toHitFromVectorResult(VectorSearchResult result, boolean includeContent) {
        RagSource source = new RagSource();
        source.setDocumentId(result.getDocumentId());
        source.setChunkId(result.getChunkId());
        source.setTitle(result.getTitle());
        source.setSourceUri(result.getSourceUri());
        source.setSectionTitle(result.getSectionTitle());
        source.setStartLine(result.getStartLine());
        source.setEndLine(result.getEndLine());
        source.setPageStart(result.getPageStart());
        source.setPageEnd(result.getPageEnd());
        source.setScore(result.getScore());

        RagChunkHit hit = new RagChunkHit();
        hit.setDocumentId(result.getDocumentId());
        hit.setChunkId(result.getChunkId());
        hit.setTitle(result.getTitle());
        hit.setScore(result.getScore());
        hit.setVectorScore(result.getScore());
        hit.setKeywordScore(0.0d);
        hit.setRerankScore(result.getScore());
        hit.setSource(source);
        hit.setMetadata(new LinkedHashMap<>(result.getMetadata()));
        hit.setContentPreview(result.getContentPreview() == null ? preview(result.getContent()) : result.getContentPreview());
        if (includeContent) {
            hit.setContent(result.getContent());
        }
        return hit;
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 160 ? content.substring(0, 160) + "..." : content;
    }

    private RagTraceStep step(String name, String status, String detail, long startedAt) {
        RagTraceStep step = new RagTraceStep();
        step.setName(name);
        step.setStatus(status);
        step.setDetail(detail);
        step.setDurationMs(System.currentTimeMillis() - startedAt);
        return step;
    }
}
