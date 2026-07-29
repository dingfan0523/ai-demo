package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagChunkHit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 可解释 RAG 重排服务。
 *
 * <p>当前使用固定权重公式综合向量分、关键词分、标题命中和 metadata 加分。
 * 这种方式不追求最好效果，但每个分数都能在响应中看到，适合学习和调参。</p>
 */
@Service
@RequiredArgsConstructor
public class ExplainableRerankService implements RerankService {

    private final RagTextTokenizer tokenizer;

    @Override
    public List<RagChunkHit> rerank(String query, List<RagChunkHit> candidates, int limit) {
        return candidates.stream()
                .peek(hit -> applyScore(query, hit))
                .sorted(Comparator.comparingDouble(RagChunkHit::getRerankScore).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 初始公式：
     * finalScore = vectorScore * 0.60 + keywordScore * 0.25 + titleMatchBoost * 0.10 + metadataBoost * 0.05
     */
    private void applyScore(String query, RagChunkHit hit) {
        double vectorScore = value(hit.getVectorScore());
        double keywordScore = value(hit.getKeywordScore());
        double titleBoost = titleMatchBoost(query, hit);
        double metadataBoost = metadataBoost(hit);
        double finalScore = vectorScore * 0.60
                + keywordScore * 0.25
                + titleBoost * 0.10
                + metadataBoost * 0.05;

        hit.setRerankScore(finalScore);
        hit.setScore(finalScore);
        if (hit.getSource() != null) {
            hit.getSource().setRerankScore(finalScore);
        }
        hit.getMetadata().put("rerankFormula", "vector*0.60 + keyword*0.25 + title*0.10 + metadata*0.05");
        hit.getMetadata().put("titleMatchBoost", titleBoost);
        hit.getMetadata().put("metadataBoost", metadataBoost);
    }

    private double titleMatchBoost(String query, RagChunkHit hit) {
        if (hit.getTitle() == null || hit.getTitle().isBlank()) {
            return 0.0d;
        }
        List<String> queryTokens = tokenizer.tokenize(query);
        List<String> titleTokens = tokenizer.tokenize(hit.getTitle() + " " + hit.getSource().getSectionTitle());
        if (queryTokens.isEmpty() || titleTokens.isEmpty()) {
            return 0.0d;
        }
        long matched = queryTokens.stream().filter(titleTokens::contains).count();
        return matched > 0 ? 1.0d : 0.0d;
    }

    private double metadataBoost(RagChunkHit hit) {
        Object tags = hit.getMetadata().get("tags");
        return tags instanceof List<?> list && !list.isEmpty() ? 1.0d : 0.0d;
    }

    private double value(Double value) {
        return value == null ? 0.0d : value;
    }
}
