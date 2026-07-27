package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagIngestResponse;

/**
 * RAG 入库服务。
 *
 * <p>编排离线阶段：解析、清洗、切分、元数据补齐、embedding 和向量写入。</p>
 */
public interface RagIngestService {

    /**
     * 执行一次文档入库。
     *
     * @param request 入库请求
     * @return 入库统计和 warning
     */
    RagIngestResponse ingest(RagIngestRequest request);
}
