package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagQueryRequest;
import com.aidemo.rag.dto.RagQueryResponse;

/**
 * RAG 答案生成服务。
 *
 * <p>负责完整在线问答链路：检索、重排、上下文组装、模型调用和引用校验。</p>
 */
public interface RagAnswerService {

    /**
     * 基于知识库上下文回答用户问题。
     *
     * @param request RAG 问答请求
     * @return 答案、引用来源和调试信息
     */
    RagQueryResponse answer(RagQueryRequest request);
}
