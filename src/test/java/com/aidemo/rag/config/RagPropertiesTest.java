package com.aidemo.rag.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagPropertiesTest {

    @Test
    void defaultsSupportLearningFirstRagFlow() {
        RagProperties properties = new RagProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getKnowledgePath()).isEqualTo("src/main/resources/knowledge");
        assertThat(properties.getChunkSize()).isEqualTo(800);
        assertThat(properties.getChunkOverlap()).isEqualTo(120);
        assertThat(properties.getTopK()).isEqualTo(8);
        assertThat(properties.getRerankTopK()).isEqualTo(4);
        assertThat(properties.getMinScore()).isEqualTo(0.3);
        assertThat(properties.getDebug().isIncludeContext()).isTrue();
    }
}
