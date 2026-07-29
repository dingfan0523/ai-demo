package com.aidemo.rag.controller;

import com.aidemo.common.Result;
import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagIngestResponse;
import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;
import com.aidemo.rag.service.RagIngestService;
import com.aidemo.rag.service.RetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 接口入口。
 *
 * <p>当前开放 Markdown 入库和检索接口，用于学习和观察 RAG 离线/在线前半段链路。
 * 答案生成接口会在后续迭代中补充。</p>
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final RagIngestService ragIngestService;
    private final RetrievalService retrievalService;

    /**
     * 导入 Markdown 文档并返回切分结果。
     *
     * @param request 入库请求
     * @return 入库统计、warning、trace steps 和调试 chunk
     */
    @PostMapping("/ingest")
    public Result<RagIngestResponse> ingest(@RequestBody @Valid RagIngestRequest request) {
        log.info("RAG离线阶段数据准备=======>");
        return Result.success(ragIngestService.ingest(request));
    }

    /**
     * 只执行检索，不调用大模型生成答案。
     *
     * @param request 检索请求
     * @return topK 命中、分数、来源和内容预览
     */
    @PostMapping("/search")
    public Result<RagSearchResponse> search(@RequestBody @Valid RagSearchRequest request) {
        log.info("RAG在线阶段数据检索=======>");
        return Result.success(retrievalService.search(request));
    }
}
