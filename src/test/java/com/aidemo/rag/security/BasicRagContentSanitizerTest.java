package com.aidemo.rag.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BasicRagContentSanitizerTest {

    private final BasicRagContentSanitizer sanitizer = new BasicRagContentSanitizer();

    @Test
    void sanitizeRemovesTocLinksAndRepeatedBlankLines() {
        String content = """
                # RAG 指南

                ## 目录
                - [入库](#入库)
                - [检索](#检索)


                ## 入库
                先解析文档。
                """;

        String sanitized = sanitizer.sanitize(content);

        assertThat(sanitized)
                .contains("# RAG 指南")
                .contains("## 入库")
                .doesNotContain("[入库](#入库)")
                .doesNotContain("\n\n\n");
    }
}
