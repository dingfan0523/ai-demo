package com.aidemo.rag.repository;

import com.aidemo.rag.model.KnowledgeChunk;

import java.util.List;

/**
 * 知识库 chunk 仓储接口。
 *
 * <p>用于保存和查询文档切片，后续可替换为内存、文件或数据库实现。</p>
 */
public interface KnowledgeChunkRepository {

    /**
     * 批量保存 chunk。
     *
     * @param chunks chunk 列表
     * @return 保存后的 chunk 列表
     */
    List<KnowledgeChunk> saveAll(List<KnowledgeChunk> chunks);

    /**
     * 查询某个文档下的全部 chunk。
     *
     * @param documentId 文档 ID
     * @return chunk 列表
     */
    List<KnowledgeChunk> findByDocumentId(String documentId);

    /**
     * 查询全部 chunk，主要用于学习阶段的本地检索和调试。
     *
     * @return chunk 列表
     */
    List<KnowledgeChunk> findAll();

    /**
     * 删除某个文档下的全部 chunk。
     *
     * @param documentId 文档 ID
     */
    void deleteByDocumentId(String documentId);
}
