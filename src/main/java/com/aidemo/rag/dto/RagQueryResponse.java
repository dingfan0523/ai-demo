package com.aidemo.rag.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 问答响应。
 *
 * <p>包含生成答案、引用来源、进入模型上下文的片段和调试步骤。</p>
 */
@Data
public class RagQueryResponse {

    private String answer;

    private String provider;

    private String model;

    private List<RagSource> sources = new ArrayList<>();

    private List<RagChunkHit> contexts = new ArrayList<>();

    private Integer confidence;

    private String traceId;

    private List<RagTraceStep> steps = new ArrayList<>();
}
