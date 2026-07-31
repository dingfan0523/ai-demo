package com.aidemo.rag.vector;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 向量检索结果。
 *
 * <p>默认只需要 chunk ID、分数和索引版本；当向量存储本身也持久化 chunk 溯源字段时，
 * 可以携带内容和来源元数据，供上层在内存仓储缺失时兜底回填。</p>
 */
@Data
public class VectorSearchResult {

    private String chunkId;

    private String documentId;

    private double score;

    private String indexVersion;

    private String content;

    private String contentPreview;

    private String title;

    private String sourceUri;

    private String sectionTitle;

    private Integer startLine;

    private Integer endLine;

    private Integer pageStart;

    private Integer pageEnd;

    private Map<String, Object> metadata = new LinkedHashMap<>();
}
