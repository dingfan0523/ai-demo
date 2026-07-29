package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagIngestResponse;
import com.aidemo.rag.repository.InMemoryKnowledgeChunkRepository;
import com.aidemo.rag.repository.InMemoryKnowledgeDocumentRepository;
import com.aidemo.rag.security.BasicRagContentSanitizer;
import com.aidemo.rag.vector.InMemoryVectorStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRagIngestServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void ingestMarkdownDirectoryAndSkipUnchangedDocument() throws IOException {
        Path markdown = tempDir.resolve("rag-guide.md");
        Files.writeString(markdown, """
                # RAG 指南
                ## 入库
                Markdown 文档会被解析、清洗和切分。

                ## 检索
                后续会接入 embedding 和向量检索。
                """);
        DefaultRagIngestService service = service();
        RagIngestRequest request = new RagIngestRequest();
        request.setSourcePath(tempDir.toString());
        request.setTags(List.of("rag", "demo"));

        RagIngestResponse first = service.ingest(request);
        RagIngestResponse second = service.ingest(request);

        assertThat(first.getDocumentCount()).isEqualTo(1);
        assertThat(first.getChunkCount()).isGreaterThan(0);
        assertThat(first.getChunks()).isNotEmpty();
        assertThat(first.getChunks().get(0).getSource().getSourceUri()).contains("rag-guide.md");
        assertThat(first.getChunks().get(0).getSource().getStartLine()).isNotNull();
        assertThat(second.getSkippedCount()).isEqualTo(1);
        assertThat(second.getDocumentCount()).isZero();
        assertThat(second.getChunkCount()).isZero();
    }

    private DefaultRagIngestService service() {
        RagProperties properties = new RagProperties();
        properties.setChunkSize(200);
        properties.setChunkOverlap(20);
        BasicRagContentSanitizer sanitizer = new BasicRagContentSanitizer();
        MarkdownDocumentParser parser = new MarkdownDocumentParser(sanitizer);
        MarkdownDocumentChunker chunker = new MarkdownDocumentChunker(properties);
        LocalHashEmbeddingService embeddingService = new LocalHashEmbeddingService(properties, new RagTextTokenizer());
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        return new DefaultRagIngestService(
                List.of(parser),
                chunker,
                new InMemoryKnowledgeDocumentRepository(),
                new InMemoryKnowledgeChunkRepository(),
                embeddingService,
                vectorStore,
                properties
        );
    }
}
