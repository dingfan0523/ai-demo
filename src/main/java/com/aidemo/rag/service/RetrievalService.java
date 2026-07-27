package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;

/**
 * RAG 检索服务。
 *
 * <p>只负责查询改写、向量召回、关键词召回和重排，不负责调用大模型生成答案。</p>
 */
public interface RetrievalService {

    /**
     * 执行一次只检索、不生成的 RAG search。
     *
     * @param request 检索请求
     * @return 召回和重排后的片段
     */
    RagSearchResponse search(RagSearchRequest request);
}
