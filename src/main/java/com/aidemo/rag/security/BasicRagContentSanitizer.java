package com.aidemo.rag.security;

import org.springframework.stereotype.Component;

/**
 * 基础 RAG 文本清洗器。
 *
 * <p>当前只做轻量清洗：统一换行、去掉 BOM、压缩多余空行和跳过常见 Markdown 目录链接。
 * 这里不做激进改写，避免学习阶段把原文语义清洗坏。</p>
 */
@Component
public class BasicRagContentSanitizer implements RagContentSanitizer {

    @Override
    public String sanitize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        StringBuilder result = new StringBuilder();
        int blankLines = 0;
        boolean inTocBlock = false;
        for (String line : normalized.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.matches("(?i)^#{1,6}\\s*(目录|table of contents)\\s*$")) {
                inTocBlock = true;
                continue;
            }
            if (inTocBlock && trimmed.matches("^-\\s*\\[[^]]+]\\(#.+\\)\\s*$")) {
                continue;
            }
            if (inTocBlock && !trimmed.isBlank()) {
                inTocBlock = false;
            }

            if (trimmed.isBlank()) {
                blankLines++;
                if (blankLines > 1) {
                    continue;
                }
            } else {
                blankLines = 0;
            }
            result.append(line.stripTrailing()).append('\n');
        }

        return result.toString().trim();
    }
}
