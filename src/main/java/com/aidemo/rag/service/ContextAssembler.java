package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagChunkHit;

import java.util.List;

/**
 * RAG 上下文组装器。
 *
 * <p>负责把重排后的 chunk 组装成模型可读的引用上下文，并控制最大长度。</p>
 */
public interface ContextAssembler {

    /**
     * 组装发送给模型的上下文文本。
     *
     * @param hits 重排后的 chunk
     * @param maxContextChars 最大上下文字符数
     * @return 带来源编号的上下文
     */
    String assemble(List<RagChunkHit> hits, int maxContextChars);
}
