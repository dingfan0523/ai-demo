package com.aidemo.rag.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析后的文档。
 *
 * <p>文档解析阶段的输出，包含标准化正文、文档元数据、warning 和诊断信息。</p>
 */
@Data
public class ParsedDocument {

    private KnowledgeDocument document;

    private String content;

    private List<String> warnings = new ArrayList<>();

    private Map<String, Object> diagnostics = new LinkedHashMap<>();
}
