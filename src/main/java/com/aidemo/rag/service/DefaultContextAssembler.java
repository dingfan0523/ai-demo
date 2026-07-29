package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagSource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 默认 RAG 上下文组装器。
 *
 * <p>它把重排后的 chunk 转成模型容易阅读的证据块，并为每个证据块分配稳定的
 * {@code [source:chunkId]} 引用标识。后续引用校验只信任这些标识，避免模型编造来源。</p>
 */
@Service
public class DefaultContextAssembler implements ContextAssembler {

    @Override
    public String assemble(List<RagChunkHit> hits, int maxContextChars) {
        StringBuilder context = new StringBuilder();
        for (RagChunkHit hit : hits) {
            String block = buildContextBlock(hit);
            if (context.length() + block.length() > maxContextChars) {
                break;
            }
            context.append(block);
        }
        return context.toString().trim();
    }

    /**
     * 构造单个证据块。
     *
     * <p>这里保留标题、章节、行号和分数，是为了让学习阶段能直观看出模型依据来自哪里；
     * 真正生产环境可以减少调试字段，只保留必要引用信息。</p>
     */
    private String buildContextBlock(RagChunkHit hit) {
        RagSource source = hit.getSource();
        StringBuilder block = new StringBuilder();
        block.append("[source:").append(hit.getChunkId()).append("]\n");
        block.append("标题: ").append(nullToEmpty(hit.getTitle())).append("\n");
        if (source != null) {
            block.append("来源: ").append(nullToEmpty(source.getSourceUri())).append("\n");
            block.append("章节: ").append(nullToEmpty(source.getSectionTitle())).append("\n");
            if (source.getStartLine() != null || source.getEndLine() != null) {
                block.append("行号: ")
                        .append(source.getStartLine() == null ? "未知" : source.getStartLine())
                        .append("-")
                        .append(source.getEndLine() == null ? "未知" : source.getEndLine())
                        .append("\n");
            }
        }
        block.append("重排分: ").append(hit.getRerankScore() == null ? 0.0d : hit.getRerankScore()).append("\n");
        block.append("内容:\n").append(nullToEmpty(selectContent(hit))).append("\n\n");
        return block.toString();
    }

    private String selectContent(RagChunkHit hit) {
        if (hit.getContent() != null && !hit.getContent().isBlank()) {
            return hit.getContent();
        }
        return hit.getContentPreview();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
