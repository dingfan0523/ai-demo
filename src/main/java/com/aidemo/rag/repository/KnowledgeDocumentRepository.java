package com.aidemo.rag.repository;

import com.aidemo.rag.model.KnowledgeDocument;

import java.util.List;
import java.util.Optional;

/**
 * 知识库文档仓储接口。
 *
 * <p>用于隔离内存、文件、数据库等不同文档存储实现。</p>
 */
public interface KnowledgeDocumentRepository {

    /**
     * 保存文档元数据。
     *
     * @param document 文档
     * @return 保存后的文档
     */
    KnowledgeDocument save(KnowledgeDocument document);

    /**
     * 根据文档 ID 查找文档。
     *
     * @param id 文档 ID
     * @return 文档
     */
    Optional<KnowledgeDocument> findById(String id);

    /**
     * 根据内容 hash 查找文档，用于避免重复入库。
     *
     * @param contentHash 内容 hash
     * @return 文档
     */
    Optional<KnowledgeDocument> findByContentHash(String contentHash);

    /**
     * 查询全部文档。
     *
     * @return 文档列表
     */
    List<KnowledgeDocument> findAll();
}
