package com.aidemo.rag.repository;

import com.aidemo.rag.model.KnowledgeDocument;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 内存版知识文档仓储。
 *
 * <p>学习阶段先用内存实现，便于观察 RAG 入库链路；后续接 pgvector 或数据库时，
 * 只需要替换仓储实现，不影响上层 service 契约。</p>
 */
@Repository
public class InMemoryKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

    private final Map<String, KnowledgeDocument> documents = new LinkedHashMap<>();

    @Override
    public synchronized KnowledgeDocument save(KnowledgeDocument document) {
        documents.put(document.getId(), document);
        return document;
    }

    @Override
    public synchronized Optional<KnowledgeDocument> findById(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    @Override
    public synchronized Optional<KnowledgeDocument> findByContentHash(String contentHash) {
        return documents.values()
                .stream()
                .filter(document -> contentHash.equals(document.getContentHash()))
                .findFirst();
    }

    @Override
    public synchronized List<KnowledgeDocument> findAll() {
        return new ArrayList<>(documents.values());
    }
}
