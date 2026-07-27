package com.aidemo.rag.vector;

import com.aidemo.rag.model.ChunkEmbedding;

import java.util.List;

/**
 * 向量存储接口。
 *
 * <p>用于隔离本地向量存储、pgvector、Milvus 等实现。RAG 服务只依赖该接口，
 * 不直接依赖某个具体向量数据库 SDK。</p>
 */
public interface VectorStore {

    /**
     * 批量保存 chunk embedding。
     *
     * @param embeddings 向量列表
     */
    void saveAll(List<ChunkEmbedding> embeddings);

    /**
     * 根据查询向量检索相似 chunk。
     *
     * @param query 向量检索条件
     * @return 相似 chunk 结果
     */
    List<VectorSearchResult> search(VectorSearchQuery query);

    /**
     * 删除某个文档对应的全部向量。
     *
     * @param documentId 文档 ID
     */
    void deleteByDocumentId(String documentId);
}
