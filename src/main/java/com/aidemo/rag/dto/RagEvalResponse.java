package com.aidemo.rag.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 评测响应。
 *
 * <p>包含汇总指标、逐用例结果和 Markdown 报告。学习阶段可以直接把报告贴到留痕文档中，
 * 用来比较参数调整前后的效果。</p>
 */
@Data
public class RagEvalResponse {

    private String traceId;

    private int totalCases;

    private int retrievalHitCount;

    private int rerankHitCount;

    private int keywordHitCount;

    private double retrievalHitRate;

    private double rerankHitRate;

    private double keywordHitRate;

    private long totalLatencyMs;

    private double averageLatencyMs;

    private String reportMarkdown;

    private String reportPath;

    private List<RagEvalCaseResult> results = new ArrayList<>();

    private List<RagTraceStep> steps = new ArrayList<>();
}
