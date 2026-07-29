package com.aidemo.rag.vector;

import com.aidemo.rag.model.ChunkEmbedding;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVectorStoreTest {

    @Test
    void searchReturnsTopKAndHonorsMinScoreAndTags() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.saveAll(List.of(
                embedding("doc-1", "chunk-1", List.of(1.0, 0.0), List.of("rag")),
                embedding("doc-2", "chunk-2", List.of(0.0, 1.0), List.of("tool"))
        ));
        VectorSearchQuery query = new VectorSearchQuery();
        query.setVector(List.of(1.0, 0.0));
        query.setTopK(5);
        query.setMinScore(0.5);
        query.setTags(List.of("rag"));

        List<VectorSearchResult> results = store.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChunkId()).isEqualTo("chunk-1");
        assertThat(results.get(0).getScore()).isEqualTo(1.0);
    }

    private ChunkEmbedding embedding(String documentId, String chunkId, List<Double> vector, List<String> tags) {
        ChunkEmbedding embedding = new ChunkEmbedding();
        embedding.setId(chunkId + "#embedding");
        embedding.setDocumentId(documentId);
        embedding.setChunkId(chunkId);
        embedding.setEmbeddingModel("local-learning-embedding");
        embedding.setVectorDimension(vector.size());
        embedding.setVector(vector);
        embedding.setIndexVersion("idx-test");
        embedding.setCreatedAt(Instant.now());
        embedding.getMetadata().put("tags", tags);
        return embedding;
    }
}
