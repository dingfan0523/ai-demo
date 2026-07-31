package com.aidemo.rag.vector;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.model.ChunkEmbedding;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchVectorStoreTest {

    private HttpServer server;
    private final List<CapturedRequest> requests = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void saveSearchAndDeleteUseElasticsearchRestApi() {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ElasticsearchVectorStore store = new ElasticsearchVectorStore(properties(), objectMapper);

        store.saveAll(List.of(embedding()));
        List<VectorSearchResult> results = store.search(query());
        store.deleteByDocumentId("doc-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChunkId()).isEqualTo("chunk-1");
        assertThat(results.get(0).getScore()).isEqualTo(0.75d);
        assertThat(results.get(0).getContent()).contains("Redis 缓存穿透");
        assertThat(results.get(0).getMetadata()).containsEntry("tags", List.of("redis"));
        assertThat(requests).extracting("method")
                .contains("HEAD", "PUT", "POST");
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.path()).contains("_bulk");
            assertThat(request.body()).contains("\"chunkId\":\"chunk-1\"");
            assertThat(request.body().trim().split("\\R")).hasSize(2);
        });
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.method()).isEqualTo("PUT");
            assertThat(request.body()).contains("\"dense_vector\"");
        });
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.path()).contains("_search");
            assertThat(request.body()).contains("cosineSimilarity");
            assertThat(request.body()).contains("\"term\":{\"tags\":\"redis\"}");
        });
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.path()).contains("_delete_by_query");
            assertThat(request.body()).contains("\"documentId\":\"doc-1\"");
        });
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().toString();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new CapturedRequest(method, path, body));

        if ("HEAD".equals(method)) {
            send(exchange, 404, "");
            return;
        }
        if ("PUT".equals(method)) {
            send(exchange, 200, "{\"acknowledged\":true}");
            return;
        }
        if (path.contains("_bulk")) {
            send(exchange, 200, "{\"errors\":false}");
            return;
        }
        if (path.contains("_search")) {
            send(exchange, 200, """
                    {
                      "hits": {
                        "hits": [
                          {
                            "_score": 1.75,
                            "_source": {
                              "documentId": "doc-1",
                              "chunkId": "chunk-1",
                              "indexVersion": "idx-test",
                              "content": "Redis 缓存穿透可以使用 Bloom Filter。",
                              "contentPreview": "Redis 缓存穿透",
                              "title": "Redis",
                              "sourceUri": "redis.md",
                              "sectionTitle": "缓存问题",
                              "startLine": 1,
                              "endLine": 5,
                              "metadata": {
                                "tags": ["redis"]
                              }
                            }
                          }
                        ]
                      }
                    }
                    """);
            return;
        }
        if (path.contains("_delete_by_query")) {
            send(exchange, 200, "{\"deleted\":1}");
            return;
        }
        send(exchange, 500, "{\"error\":\"unexpected\"}");
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private RagProperties properties() {
        RagProperties properties = new RagProperties();
        properties.getElasticsearch().setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.getElasticsearch().setIndexName("rag-test");
        properties.getElasticsearch().setRequestTimeoutSeconds(5);
        return properties;
    }

    private ChunkEmbedding embedding() {
        ChunkEmbedding embedding = new ChunkEmbedding();
        embedding.setId("embedding-1");
        embedding.setDocumentId("doc-1");
        embedding.setChunkId("chunk-1");
        embedding.setEmbeddingModel("local-learning-embedding");
        embedding.setVectorDimension(3);
        embedding.setVector(List.of(1.0d, 0.0d, 0.0d));
        embedding.setIndexVersion("idx-test");
        embedding.setCreatedAt(Instant.parse("2026-07-30T00:00:00Z"));
        embedding.setMetadata(Map.of(
                "content", "Redis 缓存穿透可以使用 Bloom Filter。",
                "contentPreview", "Redis 缓存穿透",
                "title", "Redis",
                "sourceUri", "redis.md",
                "sectionTitle", "缓存问题",
                "startLine", 1,
                "endLine", 5,
                "tags", List.of("redis")
        ));
        return embedding;
    }

    private VectorSearchQuery query() {
        VectorSearchQuery query = new VectorSearchQuery();
        query.setVector(List.of(1.0d, 0.0d, 0.0d));
        query.setTopK(3);
        query.setMinScore(0.0d);
        query.setTags(List.of("redis"));
        return query;
    }

    private record CapturedRequest(String method, String path, String body) {
    }
}
