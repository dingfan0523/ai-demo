package com.aidemo.rag.service;

import com.aidemo.rag.model.EmbeddingVector;

import java.util.List;

/**
 * Embedding 服务。
 *
 * <p>负责把文本转换成向量。该接口隔离具体模型供应商，避免 RAG 领域逻辑绑定某个 SDK。</p>
 */
public interface EmbeddingService {

    /**
     * 当前 embedding 模型名称，用于索引版本记录和后续重建判断。
     */
    String modelName();

    /**
     * 将单段文本转换成向量。
     *
     * @param text 输入文本
     * @return embedding 向量
     */
    EmbeddingVector embed(String text);

    /**
     * 批量生成向量，后续可在实现中做批处理和限流。
     *
     * @param texts 输入文本列表
     * @return embedding 向量列表
     */
    List<EmbeddingVector> embedAll(List<String> texts);
}
