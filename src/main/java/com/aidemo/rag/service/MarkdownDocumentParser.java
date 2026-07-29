package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.model.KnowledgeDocument;
import com.aidemo.rag.model.ParsedDocument;
import com.aidemo.rag.security.RagContentSanitizer;
import com.aidemo.rag.util.RagHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Markdown 文档解析器。
 *
 * <p>负责读取单个 Markdown 文件、做基础清洗、提取标题和文档级元数据。
 * 目录遍历由入库服务负责，这里只处理一个文件，职责更清楚。</p>
 */
@Component
@RequiredArgsConstructor
public class MarkdownDocumentParser implements DocumentParser {

    private final RagContentSanitizer contentSanitizer;

    @Override
    public boolean supports(String sourceType) {
        if (sourceType == null) {
            return false;
        }
        String normalized = sourceType.toLowerCase(Locale.ROOT).trim();
        return "markdown".equals(normalized) || "md".equals(normalized);
    }

    @Override
    public ParsedDocument parse(RagIngestRequest request) {
        Path path = Path.of(request.getSourcePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Markdown 来源必须是文件: " + path);
        }

        try {
            //读取markdown内容，并对格式做简单的清洗
            String rawContent = Files.readString(path, StandardCharsets.UTF_8);
            String content = contentSanitizer.sanitize(rawContent);
            String sourceUri = path.toString();

            KnowledgeDocument document = new KnowledgeDocument();
            document.setId("doc-" + RagHashUtils.shortHash(sourceUri));
            document.setTitle(resolveTitle(path, content));
            document.setSourceType("markdown");
            document.setSourceUri(sourceUri);
            document.setContentHash(RagHashUtils.sha256(content));
            document.setTags(request.getTags() == null ? new ArrayList<>() : new ArrayList<>(request.getTags()));
            document.setCreatedAt(Instant.now());
            document.setUpdatedAt(Instant.now());
            document.getMetadata().put("fileName", path.getFileName().toString());
            document.getMetadata().put("sizeBytes", Files.size(path));

            ParsedDocument parsed = new ParsedDocument();
            parsed.setDocument(document);
            parsed.setContent(content);
            parsed.getDiagnostics().put("lineCount", content.isBlank() ? 0 : content.split("\n", -1).length);
            parsed.getDiagnostics().put("parser", "markdown");
            if (content.isBlank()) {
                parsed.getWarnings().add("Markdown 文件内容为空: " + path);
            }
            return parsed;
        } catch (IOException e) {
            throw new IllegalStateException("读取 Markdown 文件失败: " + path, e);
        }
    }

    /**
     * 优先使用第一个一级标题作为文档标题；没有标题时退回文件名。
     */
    private String resolveTitle(Path path, String content) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
