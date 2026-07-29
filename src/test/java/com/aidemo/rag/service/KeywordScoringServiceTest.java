package com.aidemo.rag.service;

import com.aidemo.rag.model.KnowledgeChunk;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordScoringServiceTest {

    private final KeywordScoringService scoringService = new KeywordScoringService(new RagTextTokenizer());

    @Test
    void scoreExplainsMatchedTokens() {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setContent("Spring BeanFactory 负责 Bean 创建，RAG 检索需要 metadata。");

        KeywordScore score = scoringService.score("BeanFactory metadata 检索", chunk);

        assertThat(score.getScore()).isGreaterThan(0);
        assertThat(score.getMatchedTokens())
                .contains("beanfactory", "metadata", "检索");
    }
}
