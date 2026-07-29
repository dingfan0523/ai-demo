package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;
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

class DefaultRetrievalServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void searchReturnsRelevantMarkdownChunkAfterIngest() throws IOException {
        Files.writeString(tempDir.resolve("rag.md"), """
                # RAG 入门
                ## Markdown 入库
                RAG 入库会解析 Markdown 文档，切分 chunk，并保存 metadata。
                """);
        Files.writeString(tempDir.resolve("tool.md"), """
                # Tool 入门
                ## 工具调用
                Tool 适合执行确定性的函数调用。
                """);

        RagFixture fixture = fixture();
        RagIngestRequest ingestRequest = new RagIngestRequest();
        ingestRequest.setSourcePath(tempDir.toString());
        ingestRequest.setTags(List.of("rag"));
        fixture.ingestService().ingest(ingestRequest);

        RagSearchRequest searchRequest = new RagSearchRequest();
        searchRequest.setQuery("Markdown 文档如何入库并切分 chunk");
        searchRequest.setTopK(3);
        searchRequest.setMinScore(0.0);
        searchRequest.setTags(List.of("rag"));

        RagSearchResponse response = fixture.retrievalService().search(searchRequest);

        assertThat(response.getHits()).isNotEmpty();
        assertThat(response.getHits().get(0).getTitle()).isEqualTo("RAG 入门");
        assertThat(response.getHits().get(0).getSource().getSourceUri()).contains("rag.md");
        assertThat(response.getHits().get(0).getVectorScore()).isNotNull();
        assertThat(response.getHits().get(0).getKeywordScore()).isGreaterThan(0);
        assertThat(response.getHits().get(0).getRerankScore()).isGreaterThan(0);
        assertThat(response.getHits().get(0).getMetadata()).containsKey("matchedTokens");
        assertThat(response.getSteps()).extracting("name")
                .contains("query_embedding", "vector_search", "keyword_candidates", "explainable_rerank");
    }

    private RagFixture fixture() {
        RagProperties properties = new RagProperties();
        properties.setChunkSize(200);
        properties.setChunkOverlap(20);
        BasicRagContentSanitizer sanitizer = new BasicRagContentSanitizer();
        MarkdownDocumentParser parser = new MarkdownDocumentParser(sanitizer);
        MarkdownDocumentChunker chunker = new MarkdownDocumentChunker(properties);
        RagTextTokenizer tokenizer = new RagTextTokenizer();
        LocalHashEmbeddingService embeddingService = new LocalHashEmbeddingService(properties, tokenizer);
        KeywordScoringService keywordScoringService = new KeywordScoringService(tokenizer);
        ExplainableRerankService rerankService = new ExplainableRerankService(tokenizer);
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        InMemoryKnowledgeDocumentRepository documentRepository = new InMemoryKnowledgeDocumentRepository();
        InMemoryKnowledgeChunkRepository chunkRepository = new InMemoryKnowledgeChunkRepository();
        DefaultRagIngestService ingestService = new DefaultRagIngestService(
                List.of(parser),
                chunker,
                documentRepository,
                chunkRepository,
                embeddingService,
                vectorStore,
                properties
        );
        DefaultRetrievalService retrievalService = new DefaultRetrievalService(
                embeddingService,
                vectorStore,
                chunkRepository,
                documentRepository,
                keywordScoringService,
                rerankService,
                properties
        );
        return new RagFixture(ingestService, retrievalService);
    }

    private record RagFixture(DefaultRagIngestService ingestService,
                              DefaultRetrievalService retrievalService) {
    }
}
