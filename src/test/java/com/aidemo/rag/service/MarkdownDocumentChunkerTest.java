package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.model.KnowledgeChunk;
import com.aidemo.rag.model.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownDocumentChunkerTest {

    @Test
    void chunkKeepsHeadingMetadataAndAddsOverlapForLongSections() {
        RagProperties properties = new RagProperties();
        properties.setChunkSize(120);
        properties.setChunkOverlap(20);
        MarkdownDocumentChunker chunker = new MarkdownDocumentChunker(properties);
        KnowledgeDocument document = document();
        String content = """
                # RAG 学习
                ## 入库流程
                第一段说明 Markdown 文档需要先解析、清洗、切分，并保留来源。

                第二段说明 chunk 需要携带标题路径、行号和 hash，方便后续检索和溯源。

                第三段继续补充足够长的内容，用来触发超长章节拆分，观察 overlap 是否生效。
                """;

        List<KnowledgeChunk> chunks = chunker.chunk(document, content);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).getId()).isEqualTo("doc-test#chunk-001");
        assertThat(chunks.get(0).getTitlePath()).contains("RAG 学习");
        assertThat(chunks.get(0).getStartLine()).isNotNull();
        assertThat(chunks.get(0).getEndLine()).isNotNull();
        assertThat(chunks.get(1).getContent())
                .startsWith(chunks.get(0).getContent().substring(chunks.get(0).getContent().length() - 20));
        assertThat(chunks)
                .allSatisfy(chunk -> assertThat(chunk.getContentHash()).isNotBlank());
    }

    private KnowledgeDocument document() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId("doc-test");
        document.setTitle("RAG 学习");
        document.setSourceType("markdown");
        document.setSourceUri("docs/rag.md");
        document.setCreatedAt(Instant.now());
        return document;
    }
}
