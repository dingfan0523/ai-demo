package com.aidemo.rag.model;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档。
 *
 * <p>表示一个原始知识来源，例如一个 Markdown 文件或 PDF 文件。文档是源资产，
 * chunk 和 embedding 都应该从文档派生出来。</p>
 */
@Data
public class KnowledgeDocument {

    private String id;

    private String title;

    private String sourceType;

    private String sourceUri;

    private String contentHash;

    private List<String> tags = new ArrayList<>();

    private Map<String, Object> metadata = new LinkedHashMap<>();

    private Instant createdAt;

    private Instant updatedAt;

    private Instant deletedAt;
}
