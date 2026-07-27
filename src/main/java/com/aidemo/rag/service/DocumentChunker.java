package com.aidemo.rag.service;

import com.aidemo.rag.model.KnowledgeChunk;
import com.aidemo.rag.model.KnowledgeDocument;

import java.util.List;

/**
 * 文档切分器。
 *
 * <p>负责把解析后的完整文档切成适合检索的 chunk，并保留标题、行号、页码等溯源信息。</p>
 */
public interface DocumentChunker {

    /**
     * 将文档内容切分为多个检索片段。
     *
     * @param document 文档元数据
     * @param content 文档正文
     * @return chunk 列表
     */
    List<KnowledgeChunk> chunk(KnowledgeDocument document, String content);
}
