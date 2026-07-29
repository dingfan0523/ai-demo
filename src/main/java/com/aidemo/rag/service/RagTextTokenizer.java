package com.aidemo.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RAG 文本 token 化工具。
 *
 * <p>当前用于本地 embedding、关键词评分和 rerank。它会同时保留英文/数字 token、
 * 中文单字和中文二元组，解决中文短查询和中英文混合技术术语不容易按空格分词的问题。</p>
 */
@Component
public class RagTextTokenizer {

    /**
     * 将输入文本拆成适合学习型 RAG 检索的 token。
     *
     * @param text 输入文本
     * @return token 列表
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        StringBuilder latinToken = new StringBuilder();
        String previousCjk = null;

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isLetterOrDigit(ch) && !isCjk(ch)) {
                latinToken.append(ch);
                previousCjk = null;
                continue;
            }
            flushLatin(tokens, latinToken);
            if (isCjk(ch)) {
                String current = Character.toString(ch);
                tokens.add(current);
                if (previousCjk != null) {
                    tokens.add(previousCjk + current);
                }
                previousCjk = current;
            } else {
                previousCjk = null;
            }
        }
        flushLatin(tokens, latinToken);
        return tokens;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private void flushLatin(List<String> tokens, StringBuilder token) {
        if (!token.isEmpty()) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }
}
