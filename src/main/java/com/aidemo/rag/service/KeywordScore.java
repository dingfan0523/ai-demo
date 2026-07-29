package com.aidemo.rag.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 关键词评分结果。
 *
 * <p>用于解释某个 chunk 为什么被关键词召回或在 rerank 中得到加分。</p>
 */
@Data
public class KeywordScore {

    private double score;

    private int matchedTokenCount;

    private List<String> matchedTokens = new ArrayList<>();
}
