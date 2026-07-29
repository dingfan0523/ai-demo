package com.aidemo.rag.model;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * chunk 的向量记录。
 *
 * <p>embedding 是模型相关产物。更换 embedding 模型时，应重建向量，而不是重写源文档。</p>
 */
@Data
public class ChunkEmbedding {

    private String id;

    private String documentId;

    private String chunkId;

    private String embeddingModel;

    private Integer vectorDimension;

    private List<Double> vector = new ArrayList<>();

    private String indexVersion;

    private Instant createdAt;

    private java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
}
