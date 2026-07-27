package com.aidemo.rag.security;

import com.aidemo.rag.model.KnowledgeDocument;

/**
 * RAG 访问策略。
 *
 * <p>用于在检索前判断当前请求是否允许读取某个文档，避免越权内容被召回。</p>
 */
public interface RagAccessPolicy {

    /**
     * 判断文档是否可读。
     *
     * @param document 文档
     * @return true 表示允许读取
     */
    boolean canRead(KnowledgeDocument document);
}
