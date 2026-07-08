package com.aidemo.chat.service;

import com.aidemo.chat.core.ChatProvider;
import com.aidemo.chat.dto.ChatRequest;
import com.aidemo.chat.dto.ChatResponse;
import com.aidemo.model.config.ModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * AI 问答业务逻辑
 *
 * <p>基于策略模式调度不同模型的 ChatProvider，支持普通、流式、结构化三种问答模式</p>
 */
@Slf4j
@Service
public class ChatService {

    private final Map<String, ChatProvider> providers;
    private final ModelProperties modelProperties;

    public ChatService(Map<String, ChatProvider> providers, ModelProperties modelProperties) {
        this.providers = providers;
        this.modelProperties = modelProperties;
    }

    /**
     * 普通 AI 问答（同步纯文本）
     */
    public String chatPlain(ChatRequest request) {
        ChatProvider provider = selectProvider(request.getProvider());
        log.info("Plain chat request using provider: {}", provider.getName());
        return provider.chatPlain(request.getMessage());
    }

    /**
     * 流式 AI 问答（SSE 推送）
     */
    public SseEmitter chatStream(ChatRequest request) {
        ChatProvider provider = selectProvider(request.getProvider());
        log.info("Stream chat request using provider: {}", provider.getName());

        SseEmitter emitter = new SseEmitter(0L); // 不设置超时

        // 在异步线程中执行流式调用，避免阻塞 Servlet 线程
        new Thread(() -> {
            try {
                provider.chatStream(
                        request.getMessage(),
                        token -> {
                            try {
                                emitter.send(token);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::complete,
                        emitter::completeWithError
                );
            } catch (Exception e) {
                log.error("Stream chat error", e);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 结构化 AI 问答（返回 AiReply 实体）
     */
    public ChatResponse chatStructured(ChatRequest request) {
        ChatProvider provider = selectProvider(request.getProvider());
        log.info("Structured chat request using provider: {}", provider.getName());

        ChatResponse response = new ChatResponse();
        response.setReply(provider.chatStructured(request.getMessage()));
        response.setProvider(provider.getName());
        return response;
    }

    /**
     * 选择 provider：优先使用请求中指定的，否则使用默认配置
     */
    private ChatProvider selectProvider(String requestedProvider) {
        String name = (requestedProvider != null && !requestedProvider.isBlank())
                ? requestedProvider.toLowerCase()
                : modelProperties.getDefaultProvider().toLowerCase();

        ChatProvider provider = providers.get(name);
        if (provider != null) {
            return provider;
        }
        throw new IllegalArgumentException("Provider not found or not configured: " + name);
    }
}
