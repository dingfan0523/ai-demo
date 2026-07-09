package com.aidemo.chat.service;

import com.aidemo.chat.dto.ChatRequest;
import com.aidemo.chat.dto.ChatResponse;
import com.aidemo.chat.provider.AiProviderClient;
import com.aidemo.exception.AiServiceBusyException;
import com.aidemo.model.config.ModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AI 问答业务逻辑
 *
 * <p>基于 {@code @AiService} 声明式接口（ChatGptAssistant / DeepSeekAssistant），
 * 由 langchain4j-spring-boot-starter 自动创建代理实现。
 * 相比旧版策略模式（ChatProvider），此处省去了约 200 行接口 + 实现类代码。</p>
 *
 * <h3>两种方案对比</h3>
 * <pre>
 * ┌───────────────┬────────────────────────────────┬──────────────────────────────────────────┐
 * │               │ 旧方案：策略模式（已删除）        │ 新方案：@AiService 声明式 ✅              │
 * ├───────────────┼────────────────────────────────┼──────────────────────────────────────────┤
 * │ 模型创建      │ ModelConfig 手写 Builder        │ Stater 自动配置 / 手动 @Bean              │
 * │ 调用封装      │ ChatProvider 接口 + 4 个实现类   │ @AiService 接口 + 0 实现类                │
 * │ 消息转换      │ 手工 new UserMessage()          │ @SystemMessage/@UserMessage 注解          │
 * │ 结构化输出    │ ResponseFormat 手动配置          │ 返回 POJO 自动 JSON 解析                  │
 * │ 流式响应      │ TokenStream + 回调              │ Flux<String> 响应式                       │
 * │ 扩展新模型    │ 加 Provider 实现 + 注册          │ 加 @AiService 接口 + @Bean                │
 * └───────────────┴────────────────────────────────┴──────────────────────────────────────────┘
 * </pre>
 */
@Slf4j
@Service
public class ChatService implements InitializingBean {

    private final Map<String, AiProviderClient> providerClients;
    private final ModelProperties modelProperties;
    private final Semaphore concurrencyLimiter;

    public ChatService(List<AiProviderClient> providerClients,
                       ModelProperties modelProperties) {
        this.modelProperties = modelProperties;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toUnmodifiableMap(
                        client -> normalizeProvider(client.name()),
                        client -> client
                ));
        this.concurrencyLimiter = new Semaphore(modelProperties.getMaxConcurrentRequests());
    }

    @Override
    public void afterPropertiesSet() {
        String defaultProvider = normalizeProvider(modelProperties.getDefaultProvider());
        if (!providerClients.containsKey(defaultProvider)) {
            throw new IllegalStateException("Default AI provider is not registered: " + defaultProvider);
        }
    }

    /**
     * 普通 AI 问答（同步纯文本）
     */
    public String chatPlain(ChatRequest request) {
        AiProviderClient provider = selectProvider(request.getProvider());
        log.info("Plain chat request using provider: {}, model: {}", provider.name(), provider.modelName());
        return executeWithConcurrencyLimit(() -> provider.chat(request.getMessage()));
    }

    /**
     * 流式 AI 问答（SSE 推送）
     */
    public SseEmitter chatStream(ChatRequest request) {
        AiProviderClient provider = selectProvider(request.getProvider());
        log.info("Stream chat request using provider: {}, model: {}", provider.name(), provider.modelName());

        if (!concurrencyLimiter.tryAcquire()) {
            throw new AiServiceBusyException("AI 服务繁忙，请稍后再试");
        }

        SseEmitter emitter = new SseEmitter(modelProperties.getStreamTimeout().toMillis());
        AtomicBoolean cleaned = new AtomicBoolean(false);
        Runnable cleanup = () -> {
            if (cleaned.compareAndSet(false, true)) {
                concurrencyLimiter.release();
            }
        };
        Disposable subscription = provider.chatStream(request.getMessage())
                .subscribe(
                        token -> sendToken(emitter, token),
                        error -> {
                            cleanup.run();
                            emitter.completeWithError(error);
                        },
                        () -> {
                            cleanup.run();
                            emitter.complete();
                        }
                );

        Consumer<Throwable> cleanupWithCancel = error -> {
            subscription.dispose();
            cleanup.run();
        };
        emitter.onCompletion(() -> {
            subscription.dispose();
            cleanup.run();
        });
        emitter.onTimeout(() -> {
            cleanupWithCancel.accept(new IllegalStateException("SSE timeout"));
            emitter.complete();
        });
        emitter.onError(cleanupWithCancel);

        return emitter;
    }

    /**
     * 结构化 AI 问答（返回 AiReply 实体）
     */
    public ChatResponse chatStructured(ChatRequest request) {
        AiProviderClient provider = selectProvider(request.getProvider());
        log.info("Structured chat request using provider: {}, model: {}", provider.name(), provider.modelName());

        ChatResponse response = new ChatResponse();
        response.setReply(executeWithConcurrencyLimit(() -> provider.chatStructured(request.getMessage())));
        response.setProvider(provider.name());
        response.setModel(provider.modelName());
        return response;
    }

    /**
     * 选择 assistant：优先使用请求中指定的，否则使用默认配置
     */
    private AiProviderClient selectProvider(String requestedProvider) {
        String name = normalizeProvider(
                requestedProvider != null && !requestedProvider.isBlank()
                        ? requestedProvider
                        : modelProperties.getDefaultProvider()
        );
        AiProviderClient provider = providerClients.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found or not configured: " + name);
        }
        return provider;
    }

    private String normalizeProvider(String provider) {
        return provider.toLowerCase(Locale.ROOT).trim();
    }

    @FunctionalInterface
    private interface AiCall<T> {
        T execute();
    }

    private <T> T executeWithConcurrencyLimit(AiCall<T> call) {
        if (!concurrencyLimiter.tryAcquire()) {
            throw new AiServiceBusyException("AI 服务繁忙，请稍后再试");
        }
        try {
            return call.execute();
        } finally {
            concurrencyLimiter.release();
        }
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(token);
        } catch (IOException e) {
            log.warn("SSE send failed, completing stream");
            emitter.completeWithError(e);
        }
    }
}
