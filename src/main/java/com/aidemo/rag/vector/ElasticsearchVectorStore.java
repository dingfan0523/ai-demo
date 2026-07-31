package com.aidemo.rag.vector;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.model.ChunkEmbedding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 版向量存储。
 *
 * <p>当前按 ES 7.17 兼容方式实现：向量字段使用 {@code dense_vector}，
 * 检索使用 {@code script_score + cosineSimilarity}。这种方式是精确扫描，适合学习阶段和小数据量；
 * 后续升级 ES 8/9 原生 knn 时，可以在不改变 {@link VectorStore} 契约的前提下扩展。</p>
 */
@Component
@ConditionalOnProperty(prefix = "rag.vector-store", name = "type", havingValue = "elasticsearch")
@Slf4j
public class ElasticsearchVectorStore implements VectorStore {

    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final ObjectWriter compactJsonWriter;
    private final HttpClient httpClient;
    private volatile boolean indexReady;

    @Autowired
    public ElasticsearchVectorStore(RagProperties ragProperties, ObjectMapper objectMapper) {
        this(ragProperties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(ragProperties.getElasticsearch().getRequestTimeoutSeconds()))
                .build());
    }

    ElasticsearchVectorStore(RagProperties ragProperties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.compactJsonWriter = objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT);
        this.httpClient = httpClient;
    }

    @Override
    public void saveAll(List<ChunkEmbedding> embeddings) {
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }
        ensureIndex(embeddings.get(0).getVectorDimension());
        String bulkBody = toBulkBody(embeddings);
        HttpResponse<String> response = send("POST", indexUrl("_bulk?refresh=true"), bulkBody);
        JsonNode body = readJson(response.body());
        if (body.path("errors").asBoolean(false)) {
            throw new IllegalStateException("写入 Elasticsearch 向量索引失败: " + response.body());
        }
    }

    @Override
    public List<VectorSearchResult> search(VectorSearchQuery query) {
        ensureIndex(query.getVector() == null ? null : query.getVector().size());
        Map<String, Object> request = searchBody(query);
        HttpResponse<String> response = send("POST", indexUrl("_search"), toJson(request));
        JsonNode hits = readJson(response.body()).path("hits").path("hits");
        List<VectorSearchResult> results = new ArrayList<>();
        for (JsonNode hit : hits) {
            VectorSearchResult result = toResult(hit);
            if (result.getScore() >= query.getMinScore()) {
                results.add(result);
            }
        }
        return results;
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return;
        }
        ensureIndex(null);
        Map<String, Object> body = Map.of(
                "query", Map.of("term", Map.of("documentId", documentId))
        );
        send("POST", indexUrl("_delete_by_query?conflicts=proceed&refresh=true"), toJson(body));
    }

    /**
     * 确保 ES 索引存在。
     *
     * <p>ES 7 的 dense_vector 维度在 mapping 中固定，所以第一次创建索引时使用当前 embedding 维度。
     * 如果后续更换 embedding 维度，应新建索引或重建当前索引。</p>
     */
    private synchronized void ensureIndex(Integer vectorDimension) {
        if (indexReady) {
            return;
        }
        HttpResponse<String> head = send("HEAD", indexUrl(""), null, false);
        if (head.statusCode() == 200) {
            indexReady = true;
            return;
        }
        if (head.statusCode() != 404) {
            throw new IllegalStateException("检查 Elasticsearch 索引失败，状态码: " + head.statusCode());
        }
        int dimension = vectorDimension == null || vectorDimension <= 0 ? 128 : vectorDimension;
        send("PUT", indexUrl(""), toJson(indexMapping(dimension)));
        indexReady = true;
        log.info("RAG Elasticsearch index created: {}, dimension={}", indexName(), dimension);
    }

    private Map<String, Object> indexMapping(int dimension) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("documentId", Map.of("type", "keyword"));
        properties.put("chunkId", Map.of("type", "keyword"));
        properties.put("embeddingModel", Map.of("type", "keyword"));
        properties.put("vectorDimension", Map.of("type", "integer"));
        properties.put("indexVersion", Map.of("type", "keyword"));
        properties.put("content", Map.of("type", "text"));
        properties.put("contentPreview", Map.of("type", "text"));
        properties.put("title", Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword"))));
        properties.put("sourceUri", Map.of("type", "keyword"));
        properties.put("sectionTitle", Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword"))));
        properties.put("startLine", Map.of("type", "integer"));
        properties.put("endLine", Map.of("type", "integer"));
        properties.put("pageStart", Map.of("type", "integer"));
        properties.put("pageEnd", Map.of("type", "integer"));
        properties.put("tags", Map.of("type", "keyword"));
        properties.put("createdAt", Map.of("type", "date"));
        properties.put("metadata", Map.of("type", "object", "enabled", true));
        properties.put("vector", Map.of("type", "dense_vector", "dims", dimension));
        return Map.of("mappings", Map.of("properties", properties));
    }

    private String toBulkBody(List<ChunkEmbedding> embeddings) {
        StringBuilder body = new StringBuilder();
        for (ChunkEmbedding embedding : embeddings) {
            body.append(toJson(Map.of("index", Map.of("_id", embedding.getChunkId())))).append('\n');
            body.append(toJson(toDocument(embedding))).append('\n');
        }
        return body.toString();
    }

    private Map<String, Object> toDocument(ChunkEmbedding embedding) {
        Map<String, Object> metadata = embedding.getMetadata();
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", embedding.getId());
        document.put("documentId", embedding.getDocumentId());
        document.put("chunkId", embedding.getChunkId());
        document.put("embeddingModel", embedding.getEmbeddingModel());
        document.put("vectorDimension", embedding.getVectorDimension());
        document.put("vector", embedding.getVector());
        document.put("indexVersion", embedding.getIndexVersion());
        document.put("createdAt", embedding.getCreatedAt() == null ? null : embedding.getCreatedAt().toString());
        document.put("content", metadata.get("content"));
        document.put("contentPreview", metadata.get("contentPreview"));
        document.put("title", metadata.get("title"));
        document.put("sourceUri", metadata.get("sourceUri"));
        document.put("sectionTitle", metadata.get("sectionTitle"));
        document.put("startLine", metadata.get("startLine"));
        document.put("endLine", metadata.get("endLine"));
        document.put("pageStart", metadata.get("pageStart"));
        document.put("pageEnd", metadata.get("pageEnd"));
        document.put("tags", metadata.get("tags"));
        document.put("metadata", metadata);
        return document;
    }

    private Map<String, Object> searchBody(VectorSearchQuery query) {
        Map<String, Object> script = Map.of(
                "source", "cosineSimilarity(params.queryVector, 'vector') + 1.0",
                "params", Map.of("queryVector", query.getVector())
        );
        Map<String, Object> scriptScore = Map.of(
                "query", filterQuery(query.getTags()),
                "script", script
        );
        return Map.of(
                "size", query.getTopK(),
                "query", Map.of("script_score", scriptScore)
        );
    }

    /**
     * 标签过滤保持和内存实现一致：请求的 tags 必须全部命中。
     */
    private Map<String, Object> filterQuery(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of("match_all", Map.of());
        }
        List<Map<String, Object>> filters = tags.stream()
                .map(tag -> Map.<String, Object>of("term", Map.of("tags", tag)))
                .toList();
        return Map.of("bool", Map.of("filter", filters));
    }

    private VectorSearchResult toResult(JsonNode hit) {
        JsonNode source = hit.path("_source");
        VectorSearchResult result = new VectorSearchResult();
        result.setChunkId(text(source, "chunkId"));
        result.setDocumentId(text(source, "documentId"));
        // ES 脚本返回 cosine + 1.0，这里转回 0-1 区间，保持和内存 VectorStore 分数语义一致。
        result.setScore(Math.max(0.0d, Math.min(1.0d, hit.path("_score").asDouble(0.0d) - 1.0d)));
        result.setIndexVersion(text(source, "indexVersion"));
        result.setContent(text(source, "content"));
        result.setContentPreview(text(source, "contentPreview"));
        result.setTitle(text(source, "title"));
        result.setSourceUri(text(source, "sourceUri"));
        result.setSectionTitle(text(source, "sectionTitle"));
        result.setStartLine(integer(source, "startLine"));
        result.setEndLine(integer(source, "endLine"));
        result.setPageStart(integer(source, "pageStart"));
        result.setPageEnd(integer(source, "pageEnd"));
        JsonNode metadata = source.path("metadata");
        if (!metadata.isMissingNode() && !metadata.isNull()) {
            result.setMetadata(objectMapper.convertValue(metadata, new com.fasterxml.jackson.core.type.TypeReference<>() {
            }));
        }
        return result;
    }

    private HttpResponse<String> send(String method, String url, String body) {
        return send(method, url, body, true);
    }

    private HttpResponse<String> send(String method, String url, String body, boolean failOnError) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(ragProperties.getElasticsearch().getRequestTimeoutSeconds()));
            if ("GET".equals(method) || "HEAD".equals(method)) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (failOnError && response.statusCode() >= 300) {
                throw new IllegalStateException("调用 Elasticsearch 失败，url=" + url
                        + "，状态码=" + response.statusCode() + "，响应=" + response.body());
            }
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("调用 Elasticsearch 失败，url=" + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用 Elasticsearch 被中断，url=" + url, e);
        }
    }

    private String indexUrl(String path) {
        String baseUrl = ragProperties.getElasticsearch().getBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (path == null || path.isBlank()) {
            return normalizedBaseUrl + "/" + indexName();
        }
        return normalizedBaseUrl + "/" + indexName() + "/" + path;
    }

    private String indexName() {
        return ragProperties.getElasticsearch().getIndexName();
    }

    private String toJson(Object value) {
        try {
            return compactJsonWriter.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("序列化 Elasticsearch 请求失败", e);
        }
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw new IllegalStateException("解析 Elasticsearch 响应失败: " + body, e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }
}
