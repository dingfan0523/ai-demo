package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.model.EmbeddingVector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalHashEmbeddingServiceTest {

    private final LocalHashEmbeddingService embeddingService =
            new LocalHashEmbeddingService(new RagProperties(), new RagTextTokenizer());

    @Test
    void embedReturnsStableNormalizedVector() {
        EmbeddingVector first = embeddingService.embed("RAG 入库 Markdown chunk");
        EmbeddingVector second = embeddingService.embed("RAG 入库 Markdown chunk");

        assertThat(first.getModel()).isEqualTo("local-learning-embedding");
        assertThat(first.getDimension()).isEqualTo(128);
        assertThat(first.getValues()).isEqualTo(second.getValues());
        assertThat(first.getValues())
                .hasSize(128)
                .anySatisfy(value -> assertThat(value).isGreaterThan(0));
    }
}
