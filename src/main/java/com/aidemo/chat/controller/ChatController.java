package com.aidemo.chat.controller;

import com.aidemo.chat.dto.ChatRequest;
import com.aidemo.chat.dto.ChatResponse;
import com.aidemo.chat.service.ChatService;
import com.aidemo.common.Result;
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
public class ChatController {

    private final ChatService chatService;

    /**
     * AI 普通问答（同步纯文本）
     */
    @PostMapping
    public Result<String> chat(@RequestBody @Valid ChatRequest request) {
        log.info("Received plain chat request, provider: {}", request.getProvider());
        try {
            return Result.success(chatService.chatPlain(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Chat request failed: {}", e.getMessage());
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("Chat request error", e);
            return Result.fail(500, "AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * AI 流式问答（SSE 实时推送）
     */
    @PostMapping("/stream")
    public SseEmitter chatStream(@RequestBody @Valid ChatRequest request) {
        log.info("Received stream chat request, provider: {}", request.getProvider());
        return chatService.chatStream(request);
    }

    /**
     * AI 结构化问答（返回 AiReply 实体）
     */
    @PostMapping("/structured")
    public Result<ChatResponse> chatStructured(@RequestBody @Valid ChatRequest request) {
        log.info("Received structured chat request, provider: {}", request.getProvider());
        try {
            return Result.success(chatService.chatStructured(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Structured chat request failed: {}", e.getMessage());
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("Structured chat request error", e);
            return Result.fail(500, "AI 服务异常: " + e.getMessage());
        }
    }
}
