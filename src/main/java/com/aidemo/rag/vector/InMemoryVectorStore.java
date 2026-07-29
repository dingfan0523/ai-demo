package com.aidemo.rag.vector;

import com.aidemo.rag.model.ChunkEmbedding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存版向量存储。
 *
 * <p>用于学习阶段跑通向量检索链路。它不会持久化数据，应用重启后索引会丢失。
 * 后续 Iteration 7 会用同一个 `VectorStore` 契约替换为 pgvector 等持久化实现。</p>
 */
@Component
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, ChunkEmbedding> embeddings = new LinkedHashMap<>();

    @Override
    public synchronized void saveAll(List<ChunkEmbedding> embeddingsToSave) {
        for (ChunkEmbedding embedding : embeddingsToSave) {
            embeddings.put(embedding.getChunkId(), embedding);
        }
    }

    @Override
    public synchronized List<VectorSearchResult> search(VectorSearchQuery query) {
        return embeddings.values()
                .stream()
                .filter(embedding -> matchesTags(embedding, query.getTags()))
                .map(embedding -> toResult(embedding, query.getVector()))
                .filter(result -> result.getScore() >= query.getMinScore())
                .sorted(Comparator.comparingDouble(VectorSearchResult::getScore).reversed())
                .limit(query.getTopK())
                .toList();
    }

    @Override
    public synchronized void deleteByDocumentId(String documentId) {
        embeddings.entrySet().removeIf(entry -> documentId.equals(entry.getValue().getDocumentId()));
    }

    /**
     * 暴露当前向量数量，主要用于测试和学习阶段观察。
     */
    public synchronized int size() {
        return embeddings.size();
    }

    @SuppressWarnings("unchecked")
    private boolean matchesTags(ChunkEmbedding embedding, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        Object rawTags = embedding.getMetadata().get("tags");
        if (!(rawTags instanceof List<?> storedTags)) {
            return false;
        }
        return storedTags.containsAll(tags);
    }

    private VectorSearchResult toResult(ChunkEmbedding embedding, List<Double> queryVector) {
        VectorSearchResult result = new VectorSearchResult();
        result.setChunkId(embedding.getChunkId());
        result.setScore(cosine(queryVector, embedding.getVector()));
        result.setIndexVersion(embedding.getIndexVersion());
        return result;
    }

    /**
     * 计算余弦相似度。当前本地 embedding 已归一化，但这里仍保留完整计算，
     * 方便后续替换其他向量来源时保持结果正确。
     */
    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0d;
        }
        int length = Math.min(left.size(), right.size());
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int i = 0; i < length; i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
