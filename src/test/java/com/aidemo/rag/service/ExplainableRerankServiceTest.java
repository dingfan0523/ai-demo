package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainableRerankServiceTest {

    private final ExplainableRerankService rerankService = new ExplainableRerankService(new RagTextTokenizer());

    @Test
    void rerankCombinesVectorKeywordTitleAndMetadataScores() {
        RagChunkHit exactHit = hit("Spring BeanFactory", 0.2, 1.0, List.of("spring"));
        RagChunkHit vectorOnlyHit = hit("其他主题", 0.8, 0.0, List.of());

        List<RagChunkHit> reranked = rerankService.rerank("BeanFactory 生命周期", List.of(vectorOnlyHit, exactHit), 2);

        assertThat(reranked.get(0).getTitle()).isEqualTo("Spring BeanFactory");
        assertThat(reranked.get(0).getRerankScore()).isGreaterThan(reranked.get(1).getRerankScore());
        assertThat(reranked.get(0).getMetadata())
                .containsKey("rerankFormula")
                .containsKey("titleMatchBoost")
                .containsKey("metadataBoost");
    }

    private RagChunkHit hit(String title, double vectorScore, double keywordScore, List<String> tags) {
        RagSource source = new RagSource();
        source.setSectionTitle(title);

        RagChunkHit hit = new RagChunkHit();
        hit.setTitle(title);
        hit.setVectorScore(vectorScore);
        hit.setKeywordScore(keywordScore);
        hit.setSource(source);
        hit.getMetadata().put("tags", tags);
        return hit;
    }
}
