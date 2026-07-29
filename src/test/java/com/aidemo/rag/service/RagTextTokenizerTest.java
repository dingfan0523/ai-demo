package com.aidemo.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagTextTokenizerTest {

    private final RagTextTokenizer tokenizer = new RagTextTokenizer();

    @Test
    void tokenizeKeepsChineseBigramsAndEnglishIdentifiers() {
        List<String> tokens = tokenizer.tokenize("Spring BeanFactory 中文检索 topK");

        assertThat(tokens)
                .contains("spring", "beanfactory", "topk")
                .contains("中", "中文", "文检", "检索");
    }
}
