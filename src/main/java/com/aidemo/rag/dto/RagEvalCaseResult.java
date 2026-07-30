package com.aidemo.rag.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个 RAG 评测用例结果。
 *
 * <p>保留 top chunk、命中来源、命中关键词和失败原因，便于调参后逐条排查。</p>
 */
@Data
public class RagEvalCaseResult {

    private String caseId;

    private String question;

    private boolean retrievalHit;

    private boolean rerankHit;

    private boolean keywordHit;

    private long latencyMs;

    private List<String> expectedChunkIds = new ArrayList<>();

    private List<String> expectedSourceContains = new ArrayList<>();

    private List<String> expectedKeywords = new ArrayList<>();

    private List<String> matchedSources = new ArrayList<>();

    private List<String> matchedKeywords = new ArrayList<>();

    private List<String> topChunkIds = new ArrayList<>();

    private List<RagSource> topSources = new ArrayList<>();

    private String failureReason;

    private String notes;
}
