package com.aidemo.rag.dto;

import lombok.Data;

/**
 * RAG 引用来源。
 *
 * <p>用于把答案追溯到具体文档、章节、行号或页码。</p>
 */
@Data
public class RagSource {

    private String documentId;

    private String chunkId;

    private String title;

    private String sourceUri;

    private String sectionTitle;

    private Integer startLine;

    private Integer endLine;

    private Integer pageStart;

    private Integer pageEnd;

    private Double score;

    private Double rerankScore;
}
