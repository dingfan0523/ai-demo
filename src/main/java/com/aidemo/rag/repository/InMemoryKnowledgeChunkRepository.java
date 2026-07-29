package com.aidemo.rag.repository;

import com.aidemo.rag.model.KnowledgeChunk;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 内存版知识 chunk 仓储。
 *
 * <p>用于 Iteration 1 保存 Markdown 切分结果。它不是生产存储，只是让入库、
 * 去重和调试响应有一个最小可运行闭环。</p>
 */
@Repository
public class InMemoryKnowledgeChunkRepository implements KnowledgeChunkRepository {

    private final Map<String, KnowledgeChunk> chunks = new LinkedHashMap<>();

    @Override
    public synchronized List<KnowledgeChunk> saveAll(List<KnowledgeChunk> chunksToSave) {
        for (KnowledgeChunk chunk : chunksToSave) {
            chunks.put(chunk.getId(), chunk);
        }
        return chunksToSave;
    }

    @Override
    public synchronized List<KnowledgeChunk> findByDocumentId(String documentId) {
        return chunks.values()
                .stream()
                .filter(chunk -> documentId.equals(chunk.getDocumentId()))
                .toList();
    }

    @Override
    public synchronized Optional<KnowledgeChunk> findById(String id) {
        return Optional.ofNullable(chunks.get(id));
    }

    @Override
    public synchronized List<KnowledgeChunk> findAll() {
        return new ArrayList<>(chunks.values());
    }

    @Override
    public synchronized void deleteByDocumentId(String documentId) {
        chunks.entrySet().removeIf(entry -> documentId.equals(entry.getValue().getDocumentId()));
    }
}
