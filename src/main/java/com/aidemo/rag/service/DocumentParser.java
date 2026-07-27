package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.model.ParsedDocument;

/**
 * 文档解析器。
 *
 * <p>负责把 Markdown、PDF、txt 等来源解析成统一的 `ParsedDocument`，
 * 后续 chunker 不需要关心原始文件格式。</p>
 */
public interface DocumentParser {

    /**
     * 判断当前解析器是否支持指定来源类型。
     *
     * @param sourceType 来源类型，例如 markdown、pdf
     * @return true 表示可以解析
     */
    boolean supports(String sourceType);

    /**
     * 解析文档来源，返回原文、文档元数据和解析 warning。
     *
     * @param request 入库请求
     * @return 解析后的文档内容
     */
    ParsedDocument parse(RagIngestRequest request);
}
