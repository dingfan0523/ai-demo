package com.aidemo.rag.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 文档入库响应。
 *
 * <p>返回本次入库的统计信息、索引版本和解析 warning，便于学习阶段观察离线链路。</p>
 */
@Data
public class RagIngestResponse {

    private int documentCount;

    private int chunkCount;

    private int skippedCount;

    private String embeddingModel;

    private String indexVersion;

    private List<String> warnings = new ArrayList<>();

    private List<RagChunkHit> chunks = new ArrayList<>();

    private List<RagTraceStep> steps = new ArrayList<>();
}
