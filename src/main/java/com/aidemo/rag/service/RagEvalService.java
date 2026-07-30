package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagEvalRequest;
import com.aidemo.rag.dto.RagEvalResponse;

/**
 * RAG 评测服务。
 *
 * <p>用于执行一组手工评测用例，观察检索和重排是否命中预期来源。</p>
 */
public interface RagEvalService {

    /**
     * 执行 RAG 检索评测。
     *
     * @param request 评测请求
     * @return 汇总指标和逐用例结果
     */
    RagEvalResponse evaluate(RagEvalRequest request);
}
