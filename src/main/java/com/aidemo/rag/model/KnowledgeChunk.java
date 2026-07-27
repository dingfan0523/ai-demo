package com.aidemo.rag.model;

import lombok.Data;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识库切片。
 *
 * <p>chunk 是 RAG 的检索单位，必须保留原文、位置和章节信息，便于召回后做溯源。</p>
 */
@Data
public class KnowledgeChunk {

    private String id;

    private String documentId;

    private int chunkIndex;

    private String content;

    private String contentHash;

    private Integer tokenCount;

    private String title;

    private String titlePath;

    private String sectionTitle;

    private Integer startLine;

    private Integer endLine;

    private Integer pageStart;

    private Integer pageEnd;

    private String summary;

    private Map<String, Object> metadata = new LinkedHashMap<>();

    private Instant createdAt;
}
