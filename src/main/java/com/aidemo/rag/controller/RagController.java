package com.aidemo.rag.controller;

import com.aidemo.common.Result;
import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagIngestResponse;
import com.aidemo.rag.service.RagIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 接口入口。
 *
 * <p>当前 Iteration 1 只开放 Markdown 入库接口，用于学习和观察离线入库链路。
 * 检索和问答接口会在后续迭代中补充。</p>
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagIngestService ragIngestService;

    /**
     * 导入 Markdown 文档并返回切分结果。
     *
     * @param request 入库请求
     * @return 入库统计、warning、trace steps 和调试 chunk
     */
    @PostMapping("/ingest")
    public Result<RagIngestResponse> ingest(@RequestBody @Valid RagIngestRequest request) {
        return Result.success(ragIngestService.ingest(request));
    }
}
