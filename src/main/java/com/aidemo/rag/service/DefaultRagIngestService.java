package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagIngestResponse;
import com.aidemo.rag.dto.RagSource;
import com.aidemo.rag.dto.RagTraceStep;
import com.aidemo.rag.model.ChunkEmbedding;
import com.aidemo.rag.model.EmbeddingVector;
import com.aidemo.rag.model.KnowledgeChunk;
import com.aidemo.rag.model.KnowledgeDocument;
import com.aidemo.rag.model.ParsedDocument;
import com.aidemo.rag.repository.KnowledgeChunkRepository;
import com.aidemo.rag.repository.KnowledgeDocumentRepository;
import com.aidemo.rag.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 默认 RAG 入库服务。
 *
 * <p>当前实现 Markdown/PDF 入库和本地索引闭环：发现文档、解析、按结构切分、
 * 基于 contentHash 跳过重复内容，并把文档、chunk 和学习用向量保存到内存仓储。</p>
 */
@Service
@RequiredArgsConstructor
public class DefaultRagIngestService implements RagIngestService {

    private static final DateTimeFormatter INDEX_VERSION_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final List<DocumentParser> documentParsers;
    private final DocumentChunker documentChunker;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    @Override
    public RagIngestResponse ingest(RagIngestRequest request) {
        long startedAt = System.currentTimeMillis();
        String sourceType = normalizeSourceType(request.getSourceType());
        DocumentParser parser = selectParser(sourceType);
        List<Path> files = discoverFiles(request.getSourcePath(), sourceType);

        RagIngestResponse response = new RagIngestResponse();
        response.setEmbeddingModel(ragProperties.getEmbeddingModel());
        response.setIndexVersion("idx-" + LocalDateTime.now().format(INDEX_VERSION_FORMAT));
        response.getSteps().add(step("discover_documents", "success", "发现 " + sourceType + " 文件 " + files.size() + " 个", startedAt));

        int chunkCount = 0;
        for (Path file : files) {
            //解析后的文档内容（里面包含知识库document）
            ParsedDocument parsed = parser.parse(fileRequest(request, file, sourceType));
            response.getWarnings().addAll(parsed.getWarnings());
            response.getDiagnostics().add(diagnostic(parsed));

            //判断知识库的内容是否改变，以及是否要重写
            KnowledgeDocument document = parsed.getDocument();
            if (!request.isOverwrite() && documentRepository.findByContentHash(document.getContentHash()).isPresent()) {
                response.setSkippedCount(response.getSkippedCount() + 1);
                continue;
            }
            //保存知识库文档内容
            documentRepository.save(document);
            //删除该知识库下的切块数据
            chunkRepository.deleteByDocumentId(document.getId());
            //删除该知识库下的切块向量数据
            vectorStore.deleteByDocumentId(document.getId());
            //将内容切块并携带知识库文档部分信息（如文档id，文档来源，文档标题等）
            List<KnowledgeChunk> chunks = documentChunker.chunk(document, parsed.getContent());
            //保存检索块
            chunkRepository.saveAll(chunks);
            //对每个检索块进行向量化然后存储
            vectorStore.saveAll(toEmbeddings(document, chunks, response.getIndexVersion()));

            response.setDocumentCount(response.getDocumentCount() + 1);
            chunkCount += chunks.size();
            if (ragProperties.getDebug().isIncludeContext()) {
                response.getChunks().addAll(toChunkHits(chunks, document));
            }
        }

        response.setChunkCount(chunkCount);
        response.getSteps().add(step("chunk_documents", "success", "生成 chunk " + chunkCount + " 个", startedAt));
        response.getSteps().add(step("embed_chunks", "success", "已使用 " + embeddingService.modelName() + " 生成本地向量", startedAt));
        response.getSteps().add(step("persist_in_memory", "success", "文档、chunk 和向量已保存到内存仓储", startedAt));
        return response;
    }

    private DocumentParser selectParser(String sourceType) {
        return documentParsers.stream()
                .filter(parser -> parser.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的 RAG 文档来源类型: " + sourceType));
    }

    /**
     * 支持传入单个文件或目录。目录会按 sourceType 递归查找对应格式文件。
     */
    private List<Path> discoverFiles(String sourcePath, String sourceType) {
        Path path = Path.of(sourcePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("RAG 文档来源路径不存在: " + path);
        }
        try {
            if (Files.isRegularFile(path)) {
                if (!matchesSourceType(path, sourceType)) {
                    throw new IllegalArgumentException("文件类型与 RAG 来源类型不匹配: " + path + "，sourceType=" + sourceType);
                }
                return List.of(path);
            }
            try (var paths = Files.walk(path)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(file -> matchesSourceType(file, sourceType))
                        .sorted(Comparator.comparing(Path::toString))
                        .toList();
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("扫描 RAG 文档来源失败: " + path, e);
        }
    }

    private boolean isMarkdown(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".md") || fileName.endsWith(".markdown");
    }

    private boolean matchesSourceType(Path path, String sourceType) {
        if ("pdf".equals(sourceType)) {
            return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf");
        }
        if ("markdown".equals(sourceType) || "md".equals(sourceType)) {
            return isMarkdown(path);
        }
        return false;
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "markdown";
        }
        String normalized = sourceType.toLowerCase(Locale.ROOT).trim();
        return "md".equals(normalized) ? "markdown" : normalized;
    }

    private RagIngestRequest fileRequest(RagIngestRequest request, Path file, String sourceType) {
        RagIngestRequest fileRequest = new RagIngestRequest();
        fileRequest.setSourceType(sourceType);
        fileRequest.setSourcePath(file.toString());
        fileRequest.setTags(request.getTags() == null ? new ArrayList<>() : new ArrayList<>(request.getTags()));
        fileRequest.setOverwrite(request.isOverwrite());
        return fileRequest;
    }

    private Map<String, Object> diagnostic(ParsedDocument parsed) {
        Map<String, Object> diagnostic = new java.util.LinkedHashMap<>(parsed.getDiagnostics());
        diagnostic.put("documentId", parsed.getDocument().getId());
        diagnostic.put("title", parsed.getDocument().getTitle());
        diagnostic.put("sourceUri", parsed.getDocument().getSourceUri());
        diagnostic.put("sourceType", parsed.getDocument().getSourceType());
        return diagnostic;
    }

    private List<RagChunkHit> toChunkHits(List<KnowledgeChunk> chunks, KnowledgeDocument document) {
        return chunks.stream()
                .map(chunk -> toChunkHit(chunk, document))
                .toList();
    }

    /**
     * 为 chunk 生成 embedding 记录。
     *
     * <p>这里把 sourceUri 和 tags 放入向量 metadata，便于本地 VectorStore 做基础过滤。
     * 后续接 pgvector 时，这些字段也会成为 metadata filter 的候选字段。</p>
     */
    private List<ChunkEmbedding> toEmbeddings(KnowledgeDocument document, List<KnowledgeChunk> chunks, String indexVersion) {
        List<EmbeddingVector> vectors = embeddingService.embedAll(chunks.stream()
                .map(KnowledgeChunk::getContent)
                .toList());
        List<ChunkEmbedding> embeddings = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            EmbeddingVector vector = vectors.get(i);

            ChunkEmbedding embedding = new ChunkEmbedding();
            embedding.setId(chunk.getId() + "#embedding-" + embeddingService.modelName());
            embedding.setDocumentId(document.getId());
            embedding.setChunkId(chunk.getId());
            embedding.setEmbeddingModel(vector.getModel());
            embedding.setVectorDimension(vector.getDimension());
            embedding.setVector(vector.getValues());
            embedding.setIndexVersion(indexVersion);
            embedding.setCreatedAt(chunk.getCreatedAt());
            embedding.getMetadata().put("sourceUri", document.getSourceUri());
            embedding.getMetadata().put("tags", document.getTags());
            embeddings.add(embedding);
        }
        return embeddings;
    }

    private RagChunkHit toChunkHit(KnowledgeChunk chunk, KnowledgeDocument document) {
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

        RagChunkHit hit = new RagChunkHit();
        hit.setDocumentId(document.getId());
        hit.setChunkId(chunk.getId());
        hit.setTitle(document.getTitle());
        hit.setContent(chunk.getContent());
        hit.setContentPreview(chunk.getContent().length() > 160
                ? chunk.getContent().substring(0, 160) + "..."
                : chunk.getContent());
        hit.setSource(source);
        hit.setMetadata(chunk.getMetadata());
        return hit;
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
