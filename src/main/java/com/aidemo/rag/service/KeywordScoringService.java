package com.aidemo.rag.service;

import com.aidemo.rag.model.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 关键词评分服务。
 *
 * <p>当前实现一个简化版关键词匹配分数。它不是完整 BM25，但足够让学习阶段观察
 * “精确术语命中”和“语义向量召回”之间的差异。</p>
 */
@Service
@RequiredArgsConstructor
public class KeywordScoringService {

    private final RagTextTokenizer tokenizer;

    /**
     * 计算查询与 chunk 的关键词匹配程度。
     *
     * @param query 用户查询
     * @param chunk 候选 chunk
     * @return 0 到 1 之间的关键词分数
     */
    public KeywordScore score(String query, KnowledgeChunk chunk) {
        Set<String> queryTokens = new LinkedHashSet<>(tokenizer.tokenize(query));
        if (queryTokens.isEmpty() || chunk == null || chunk.getContent() == null) {
            return new KeywordScore();
        }

        Set<String> contentTokens = new LinkedHashSet<>(tokenizer.tokenize(chunk.getContent()));
        List<String> matched = queryTokens.stream()
                .filter(contentTokens::contains)
                .toList();

        KeywordScore score = new KeywordScore();
        score.setMatchedTokenCount(matched.size());
        score.setMatchedTokens(matched);
        score.setScore((double) matched.size() / queryTokens.size());
        return score;
    }
}
