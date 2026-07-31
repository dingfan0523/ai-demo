package com.aidemo.rag.controller;

import com.aidemo.common.Result;
import com.aidemo.rag.dto.RagEvalRequest;
import com.aidemo.rag.dto.RagEvalResponse;
import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagIngestResponse;
import com.aidemo.rag.dto.RagQueryRequest;
import com.aidemo.rag.dto.RagQueryResponse;
import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;
import com.aidemo.rag.service.RagAnswerService;
import com.aidemo.rag.service.RagEvalService;
import com.aidemo.rag.service.RagIngestService;
import com.aidemo.rag.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * <p>当前开放文档入库、检索、问答和评测接口，用于学习和观察 RAG 离线/在线完整链路。</p>
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RAG 学习", description = "RAG 入库、检索、问答和评测调试接口")
public class RagController {

    private final RagIngestService ragIngestService;
    private final RetrievalService retrievalService;
    private final RagAnswerService ragAnswerService;
    private final RagEvalService ragEvalService;

    /**
     * 导入 Markdown 文档并返回切分结果。
     *
     * @param request 入库请求
     * @return 入库统计、warning、trace steps 和调试 chunk
     */
    @PostMapping("/ingest")
    @Operation(summary = "文档入库", description = "导入 Markdown、文本或 PDF 文档，返回切分、向量化和入库调试信息。")
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
    @Operation(summary = "只检索不生成", description = "执行召回和重排，不调用大模型，适合排查检索质量。")
    public Result<RagSearchResponse> search(@RequestBody @Valid RagSearchRequest request) {
        log.info("RAG在线阶段数据检索=======>");
        return Result.success(retrievalService.search(request));
    }

    /**
     * 执行完整 RAG 问答：检索、上下文组装、模型生成和引用校验。
     *
     * @param request RAG 问答请求
     * @return 带答案、上下文和可信来源的 RAG 响应
     */
    @PostMapping("/query")
    @Operation(summary = "RAG 问答", description = "执行检索、重排、上下文组装、模型生成和引用校验。")
    public Result<RagQueryResponse> query(@RequestBody @Valid RagQueryRequest request) {
        log.info("RAG在线阶段答案生成=======>");
        return Result.success(ragAnswerService.answer(request));
    }

    /**
     * 执行 RAG 检索评测，用于观察召回和重排是否命中手工期望来源。
     *
     * @param request 评测请求
     * @return 汇总指标、逐用例结果和 Markdown 报告
     */
    @PostMapping("/evaluate")
    @Operation(summary = "RAG 检索评测", description = "执行手工评测用例，观察召回和重排是否命中预期来源。")
    public Result<RagEvalResponse> evaluate(@RequestBody @Valid RagEvalRequest request) {
        log.info("RAG评测阶段执行=======>");
        return Result.success(ragEvalService.evaluate(request));
    }
}
