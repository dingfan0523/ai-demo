package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.model.KnowledgeDocument;
import com.aidemo.rag.model.ParsedDocument;
import com.aidemo.rag.security.RagContentSanitizer;
import com.aidemo.rag.util.RagHashUtils;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PDF 文档解析器。
 *
 * <p>当前只支持文本型 PDF：使用 PDFBox 抽取每页文本，并把每页整理成 Markdown 风格章节。
 * 如果 PDF 几乎抽不出文本，会返回中文 warning，提示它可能是扫描件或图片型 PDF。</p>
 */
@Component
@RequiredArgsConstructor
public class PdfDocumentParser implements DocumentParser {

    private final RagContentSanitizer contentSanitizer;

    @Override
    public boolean supports(String sourceType) {
        return sourceType != null && "pdf".equals(sourceType.toLowerCase(Locale.ROOT).trim());
    }

    @Override
    public ParsedDocument parse(RagIngestRequest request) {
        Path path = Path.of(request.getSourcePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("PDF 来源必须是文件: " + path);
        }

        try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
            PdfContent pdfContent = extractContent(path, pdf);
            KnowledgeDocument document = buildDocument(request, path, pdf, pdfContent);

            ParsedDocument parsed = new ParsedDocument();
            parsed.setDocument(document);
            parsed.setContent(pdfContent.content());
            parsed.getDiagnostics().put("parser", "pdfbox");
            parsed.getDiagnostics().put("pageCount", pdf.getNumberOfPages());
            parsed.getDiagnostics().put("textPageCount", pdfContent.textPageCount());
            parsed.getDiagnostics().put("blankPageCount", pdf.getNumberOfPages() - pdfContent.textPageCount());
            parsed.getDiagnostics().put("extractedTextChars", pdfContent.extractedTextChars());
            parsed.getDiagnostics().put("pageLineRanges", pdfContent.pageLineRanges());
            parsed.getWarnings().addAll(buildWarnings(path, pdf, pdfContent));
            return parsed;
        } catch (IOException e) {
            throw new IllegalStateException("读取 PDF 文件失败: " + path, e);
        }
    }

    /**
     * 逐页抽取文本并记录页码到行号的映射。
     *
     * <p>把每页转成 `## 第 N 页` 是为了复用现有 Markdown 切分策略，同时让 chunk 能追溯页码。</p>
     */
    private PdfContent extractContent(Path path, PDDocument pdf) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        StringBuilder content = new StringBuilder();
        List<Map<String, Integer>> pageLineRanges = new ArrayList<>();
        int currentLine = 1;
        int textPageCount = 0;
        int extractedTextChars = 0;

        content.append("# ").append(resolveTitle(path, pdf)).append("\n\n");
        currentLine += 2;

        for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            String pageText = contentSanitizer.sanitize(stripper.getText(pdf)).trim();
            if (!pageText.isBlank()) {
                textPageCount++;
                extractedTextChars += pageText.length();
            }

            int startLine = currentLine;
            content.append("## 第 ").append(page).append(" 页\n");
            currentLine++;
            if (!pageText.isBlank()) {
                for (String line : pageText.split("\n", -1)) {
                    content.append(line).append("\n");
                    currentLine++;
                }
            }
            content.append("\n");
            currentLine++;

            Map<String, Integer> range = new LinkedHashMap<>();
            range.put("page", page);
            range.put("startLine", startLine);
            range.put("endLine", currentLine - 1);
            pageLineRanges.add(range);
        }
        return new PdfContent(content.toString().trim(), pageLineRanges, textPageCount, extractedTextChars);
    }

    private KnowledgeDocument buildDocument(RagIngestRequest request, Path path, PDDocument pdf, PdfContent pdfContent) throws IOException {
        KnowledgeDocument document = new KnowledgeDocument();
        String sourceUri = path.toString();
        document.setId("doc-" + RagHashUtils.shortHash(sourceUri));
        document.setTitle(resolveTitle(path, pdf));
        document.setSourceType("pdf");
        document.setSourceUri(sourceUri);
        document.setContentHash(RagHashUtils.sha256(pdfContent.content()));
        document.setTags(request.getTags() == null ? new ArrayList<>() : new ArrayList<>(request.getTags()));
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        document.getMetadata().put("fileName", path.getFileName().toString());
        document.getMetadata().put("sizeBytes", Files.size(path));
        document.getMetadata().put("pageCount", pdf.getNumberOfPages());
        document.getMetadata().put("textPageCount", pdfContent.textPageCount());
        document.getMetadata().put("extractedTextChars", pdfContent.extractedTextChars());
        document.getMetadata().put("pageLineRanges", pdfContent.pageLineRanges());
        document.getMetadata().put("parser", "pdfbox");
        return document;
    }

    private List<String> buildWarnings(Path path, PDDocument pdf, PdfContent pdfContent) {
        List<String> warnings = new ArrayList<>();
        if (pdfContent.textPageCount() == 0 || pdfContent.extractedTextChars() < 20) {
            warnings.add("PDF 未抽取到足够文本，可能是扫描件或图片型 PDF: " + path);
        }
        if (pdfContent.textPageCount() < pdf.getNumberOfPages()) {
            warnings.add("PDF 存在空文本页，可能包含图片、扫描页或复杂版面: " + path);
        }
        return warnings;
    }

    private String resolveTitle(Path path, PDDocument pdf) {
        String title = pdf.getDocumentInformation() == null ? null : pdf.getDocumentInformation().getTitle();
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private record PdfContent(String content,
                              List<Map<String, Integer>> pageLineRanges,
                              int textPageCount,
                              int extractedTextChars) {
    }
}
