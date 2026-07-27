package com.aidemo.rag.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RAG 召回命中的 chunk。
 *
 * <p>同时保留向量分、关键词分和重排分，方便学习阶段解释排序结果。</p>
 */
@Data
public class RagChunkHit {

    private String chunkId;

    private String documentId;

    private String title;

    private Double score;

    private Double vectorScore;

    private Double keywordScore;

    private Double rerankScore;

    private String content;

    private String contentPreview;

    private RagSource source;

    private Map<String, Object> metadata = new LinkedHashMap<>();
}
