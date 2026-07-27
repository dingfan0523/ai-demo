package com.aidemo.rag.dto;

import lombok.Data;

/**
 * RAG 链路步骤记录。
 *
 * <p>用于观察一次请求经历了哪些阶段，例如 query rewrite、retrieval、rerank、answer generation。</p>
 */
@Data
public class RagTraceStep {

    private String name;

    private String status;

    private String detail;

    private Long durationMs;
}
