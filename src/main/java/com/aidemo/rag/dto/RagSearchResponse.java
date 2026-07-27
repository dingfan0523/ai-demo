package com.aidemo.rag.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索响应。
 *
 * <p>只展示召回和重排结果，不包含模型生成答案，用于判断检索质量。</p>
 */
@Data
public class RagSearchResponse {

    private String originalQuery;

    private String rewrittenQuery;

    private List<RagChunkHit> hits = new ArrayList<>();

    private List<RagTraceStep> steps = new ArrayList<>();

    private String traceId;
}
