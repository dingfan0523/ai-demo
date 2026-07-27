package com.aidemo.rag.security;

/**
 * RAG 内容清洗器。
 *
 * <p>用于在文档入库或 prompt 组装前清理敏感内容、明显噪声或危险片段。</p>
 */
public interface RagContentSanitizer {

    /**
     * 清洗文本内容。
     *
     * @param content 原始内容
     * @return 清洗后的内容
     */
    String sanitize(String content);
}
