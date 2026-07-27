package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.model.KnowledgeChunk;
import com.aidemo.rag.model.KnowledgeDocument;
import com.aidemo.rag.util.RagHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 文档切分器。
 *
 * <p>切分策略是“标题结构优先，长度兜底”。先按 Markdown 标题形成章节，
 * 再对超长章节按配置长度拆分，并给相邻 chunk 加少量 overlap，降低上下文断裂风险。</p>
 */
@Component
@RequiredArgsConstructor
public class MarkdownDocumentChunker implements DocumentChunker {

    private final RagProperties ragProperties;

    @Override
    public List<KnowledgeChunk> chunk(KnowledgeDocument document, String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<Section> sections = splitSections(content);
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (Section section : sections) {
            chunks.addAll(splitSection(document, section, chunks.size()));
        }
        return applyOverlapAndIds(document, chunks);
    }

    /**
     * 按 Markdown 标题拆出章节。这样 chunk 能保留标题路径，后续引用更容易读懂。
     */
    private List<Section> splitSections(String content) {
        String[] lines = content.split("\n", -1);
        List<Section> sections = new ArrayList<>();
        List<String> titleStack = new ArrayList<>();
        Section current = null;

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i];
            Heading heading = parseHeading(line);
            if (heading != null) {
                if (current != null && current.hasBodyContent()) {
                    sections.add(current);
                }
                updateTitleStack(titleStack, heading);
                current = new Section(heading.title(), String.join(" > ", titleStack), lineNumber);
            }

            if (current == null) {
                current = new Section("正文", "正文", lineNumber);
            }
            current.append(line, lineNumber);
        }

        if (current != null && current.hasBodyContent()) {
            sections.add(current);
        }
        return sections;
    }

    /**
     * 超长章节按长度窗口兜底拆分。当前按字符近似控制，后续可替换为 token 计数器。
     */
    private List<KnowledgeChunk> splitSection(KnowledgeDocument document, Section section, int chunkOffset) {
        List<KnowledgeChunk> result = new ArrayList<>();
        String text = section.content().trim();
        int chunkSize = ragProperties.getChunkSize();

        if (text.length() <= chunkSize) {
            result.add(createChunk(document, section, text, chunkOffset));
            return result;
        }

        int cursor = 0;
        while (cursor < text.length()) {
            int end = Math.min(text.length(), cursor + chunkSize);
            int adjustedEnd = adjustToReadableBoundary(text, cursor, end);
            String chunkText = text.substring(cursor, adjustedEnd).trim();
            if (!chunkText.isBlank()) {
                result.add(createChunk(document, section, chunkText, chunkOffset + result.size()));
            }
            cursor = adjustedEnd;
        }
        return result;
    }

    /**
     * 尽量在段落或句子边界截断，避免把一个说明句硬切到两个 chunk 中。
     */
    private int adjustToReadableBoundary(String text, int start, int end) {
        if (end >= text.length()) {
            return end;
        }
        int paragraphBreak = text.lastIndexOf("\n\n", end);
        if (paragraphBreak > start + ragProperties.getChunkSize() / 2) {
            return paragraphBreak;
        }
        int sentenceBreak = Math.max(text.lastIndexOf('。', end), text.lastIndexOf('.', end));
        if (sentenceBreak > start + ragProperties.getChunkSize() / 2) {
            return sentenceBreak + 1;
        }
        return end;
    }

    /**
     * 为第二个及之后的 chunk 添加上一段尾部 overlap，并生成稳定 chunk ID。
     */
    private List<KnowledgeChunk> applyOverlapAndIds(KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        int overlap = ragProperties.getChunkOverlap();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            if (i > 0 && overlap > 0) {
                String previousContent = chunks.get(i - 1).getContent();
                String prefix = previousContent.substring(Math.max(0, previousContent.length() - overlap));
                chunk.setContent((prefix + "\n" + chunk.getContent()).trim());
            }
            chunk.setChunkIndex(i);
            chunk.setId(document.getId() + "#chunk-" + String.format("%03d", i + 1));
            chunk.setContentHash(RagHashUtils.sha256(chunk.getContent()));
            chunk.setTokenCount(chunk.getContent().length());
        }
        return chunks;
    }

    private KnowledgeChunk createChunk(KnowledgeDocument document, Section section, String content, int chunkIndex) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setDocumentId(document.getId());
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setTitle(document.getTitle());
        chunk.setTitlePath(section.titlePath());
        chunk.setSectionTitle(section.title());
        chunk.setStartLine(section.startLine());
        chunk.setEndLine(section.endLine());
        chunk.setCreatedAt(Instant.now());
        chunk.getMetadata().put("sourceUri", document.getSourceUri());
        chunk.getMetadata().put("sourceType", document.getSourceType());
        chunk.getMetadata().put("titlePath", section.titlePath());
        return chunk;
    }

    private Heading parseHeading(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("#")) {
            return null;
        }
        int level = 0;
        while (level < trimmed.length() && trimmed.charAt(level) == '#') {
            level++;
        }
        if (level == 0 || level > 6 || level >= trimmed.length() || trimmed.charAt(level) != ' ') {
            return null;
        }
        return new Heading(level, trimmed.substring(level + 1).trim());
    }

    private void updateTitleStack(List<String> titleStack, Heading heading) {
        while (titleStack.size() >= heading.level()) {
            titleStack.remove(titleStack.size() - 1);
        }
        titleStack.add(heading.title());
    }

    private record Heading(int level, String title) {
    }

    private static final class Section {
        private final String title;
        private final String titlePath;
        private final int startLine;
        private int endLine;
        private boolean hasBodyContent;
        private final StringBuilder content = new StringBuilder();

        private Section(String title, String titlePath, int startLine) {
            this.title = title;
            this.titlePath = titlePath;
            this.startLine = startLine;
            this.endLine = startLine;
        }

        private void append(String line, int lineNumber) {
            content.append(line).append('\n');
            endLine = lineNumber;
            // 只有标题没有正文的章节不单独生成 chunk，避免产生没有检索价值的小片段。
            if (!line.trim().isBlank() && !line.trim().startsWith("#")) {
                hasBodyContent = true;
            }
        }

        private String title() {
            return title;
        }

        private String titlePath() {
            return titlePath;
        }

        private int startLine() {
            return startLine;
        }

        private int endLine() {
            return endLine;
        }

        private String content() {
            return content.toString();
        }

        private boolean hasBodyContent() {
            return hasBodyContent;
        }
    }
}
