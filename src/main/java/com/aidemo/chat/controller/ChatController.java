package com.aidemo.chat.controller;

import com.aidemo.chat.dto.ChatRequest;
import com.aidemo.chat.dto.ChatResponse;
import com.aidemo.chat.service.ChatService;
import com.aidemo.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 问答接口
 *
 * <p>提供三种问答模式：普通文本、流式推送（SSE）、结构化 JSON</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 问答", description = "普通问答、SSE 流式问答和结构化问答调试接口")
public class ChatController {

    private final ChatService chatService;

    /**
     * AI 普通问答（同步纯文本）
     */
    @PostMapping
    @Operation(summary = "普通问答", description = "同步调用当前默认或指定模型，返回纯文本答案。")
    public Result<String> chat(@RequestBody @Valid ChatRequest request) {
        log.info("Received plain chat request, provider: {}", request.getProvider());
        return Result.success(chatService.chatPlain(request));
    }

    /**
     * AI 流式问答（SSE 实时推送）
     */
    @PostMapping("/stream")
    @Operation(summary = "流式问答", description = "通过 SSE 实时推送模型生成内容。")
    public SseEmitter chatStream(@RequestBody @Valid ChatRequest request) {
        log.info("Received stream chat request, provider: {}", request.getProvider());
        return chatService.chatStream(request);
    }

    /**
     * AI 结构化问答（返回 AiReply 实体）
     */
    @PostMapping("/structured")
    @Operation(summary = "结构化问答", description = "调用模型并解析为结构化响应对象，便于观察结构化输出效果。")
    public Result<ChatResponse> chatStructured(@RequestBody @Valid ChatRequest request) {
        log.info("Received structured chat request, provider: {}", request.getProvider());
        return Result.success(chatService.chatStructured(request));
    }
}
