package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagEvalCase;
import com.aidemo.rag.dto.RagEvalRequest;
import com.aidemo.rag.dto.RagEvalResponse;
import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;
import com.aidemo.rag.dto.RagSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRagEvalServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void evaluateSummarizesRetrievalRerankAndKeywordHits() {
        DefaultRagEvalService service = service(searchRequest -> response(List.of(
                hit("chunk-tool", "tool.md", "Tool 调用适合执行函数。"),
                hit("chunk-redis", "redis.md", "Bloom Filter 可以缓解 Redis 缓存穿透。")
        )));
        RagEvalRequest request = new RagEvalRequest();
        request.setTopK(2);
        request.setRerankTopK(1);
        request.setCases(List.of(evalCase()));

        RagEvalResponse response = service.evaluate(request);

        assertThat(response.getTotalCases()).isEqualTo(1);
        assertThat(response.getRetrievalHitCount()).isEqualTo(1);
        assertThat(response.getRerankHitCount()).isZero();
        assertThat(response.getKeywordHitCount()).isEqualTo(1);
        assertThat(response.getRetrievalHitRate()).isEqualTo(1.0d);
        assertThat(response.getRerankHitRate()).isEqualTo(0.0d);
        assertThat(response.getResults().get(0).getFailureReason()).contains("rerankTopK 未命中期望来源");
        assertThat(response.getReportMarkdown()).contains("RAG 评测报告");
    }

    @Test
    void evaluateLoadsCasesFromJsonFile() throws IOException {
        Path casesFile = tempDir.resolve("rag-eval-cases.json");
        Path reportFile = tempDir.resolve("rag-eval-report.md");
        Files.writeString(casesFile, """
                {
                  "cases": [
                    {
                      "caseId": "redis-cache-penetration",
                      "question": "Redis 缓存穿透怎么处理？",
                      "expectedSourceContains": ["redis.md"],
                      "expectedKeywords": ["Bloom Filter"]
                    }
                  ]
                }
                """);
        DefaultRagEvalService service = service(searchRequest -> response(List.of(
                hit("chunk-redis", "redis.md", "Bloom Filter 可以缓解 Redis 缓存穿透。")
        )));
        RagEvalRequest request = new RagEvalRequest();
        request.setCasesPath(casesFile.toString());
        request.setReportPath(reportFile.toString());

        RagEvalResponse response = service.evaluate(request);

        assertThat(response.getTotalCases()).isEqualTo(1);
        assertThat(response.getRetrievalHitRate()).isEqualTo(1.0d);
        assertThat(response.getRerankHitRate()).isEqualTo(1.0d);
        assertThat(response.getResults().get(0).getCaseId()).isEqualTo("redis-cache-penetration");
        assertThat(response.getReportPath()).isEqualTo(reportFile.toAbsolutePath().normalize().toString());
        assertThat(Files.readString(reportFile)).contains("RAG 评测报告");
    }

    private RagEvalCase evalCase() {
        RagEvalCase evalCase = new RagEvalCase();
        evalCase.setCaseId("redis-cache-penetration");
        evalCase.setQuestion("Redis 缓存穿透怎么处理？");
        evalCase.setExpectedSourceContains(List.of("redis.md"));
        evalCase.setExpectedKeywords(List.of("Bloom Filter"));
        return evalCase;
    }

    private DefaultRagEvalService service(RetrievalService retrievalService) {
        RagProperties properties = new RagProperties();
        properties.setTopK(3);
        properties.setRerankTopK(2);
        properties.setMinScore(0.0d);
        return new DefaultRagEvalService(retrievalService, properties, new ObjectMapper());
    }

    private RagSearchResponse response(List<RagChunkHit> hits) {
        RagSearchResponse response = new RagSearchResponse();
        response.setTraceId("search-test");
        response.setHits(hits);
        return response;
    }

    private RagChunkHit hit(String chunkId, String sourceUri, String content) {
        RagSource source = new RagSource();
        source.setChunkId(chunkId);
        source.setDocumentId("doc-" + chunkId);
        source.setTitle(sourceUri);
        source.setSourceUri(sourceUri);
        source.setSectionTitle("测试章节");

        RagChunkHit hit = new RagChunkHit();
        hit.setChunkId(chunkId);
        hit.setDocumentId(source.getDocumentId());
        hit.setTitle(sourceUri);
        hit.setContent(content);
        hit.setContentPreview(content);
        hit.setSource(source);
        hit.setRerankScore(0.8d);
        return hit;
    }

    @FunctionalInterface
    private interface RetrievalStub extends RetrievalService {
        @Override
        RagSearchResponse search(RagSearchRequest request);
    }
}
