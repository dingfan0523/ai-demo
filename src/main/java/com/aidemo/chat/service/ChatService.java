package com.aidemo.chat.service;

import com.aidemo.chat.assistant.ChatGptAssistant;
import com.aidemo.chat.assistant.DeepSeekAssistant;
import com.aidemo.chat.dto.ChatRequest;
import com.aidemo.chat.dto.ChatResponse;
import com.aidemo.model.config.ModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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
public class ChatService {

    private final ChatGptAssistant chatGptAssistant;
    private final DeepSeekAssistant deepSeekAssistant;
    private final ModelProperties modelProperties;

    public ChatService(ChatGptAssistant chatGptAssistant,
                       DeepSeekAssistant deepSeekAssistant,
                       ModelProperties modelProperties) {
        this.chatGptAssistant = chatGptAssistant;
        this.deepSeekAssistant = deepSeekAssistant;
        this.modelProperties = modelProperties;
    }

    /**
     * 普通 AI 问答（同步纯文本）
     */
    public String chatPlain(ChatRequest request) {
        Assistant assistant = selectAssistant(request.getProvider());
        log.info("Plain chat request using provider: {}", assistant.name());
        return assistant.chat(request.getMessage());
    }

    /**
     * 流式 AI 问答（SSE 推送）
     */
    public SseEmitter chatStream(ChatRequest request) {
        Assistant assistant = selectAssistant(request.getProvider());
        log.info("Stream chat request using provider: {}", assistant.name());

        SseEmitter emitter = new SseEmitter(0L); // 不设置超时

        // 使用 Flux 响应式流，转换为 SSE 推送
        assistant.chatStream(request.getMessage())
                .subscribe(
                        token -> {
                            try {
                                emitter.send(token);
                            } catch (IOException e) {
                                log.error("SSE send error", e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );

        return emitter;
    }

    /**
     * 结构化 AI 问答（返回 AiReply 实体）
     */
    public ChatResponse chatStructured(ChatRequest request) {
        Assistant assistant = selectAssistant(request.getProvider());
        log.info("Structured chat request using provider: {}", assistant.name());

        ChatResponse response = new ChatResponse();
        response.setReply(assistant.chatStructured(request.getMessage()));
        response.setProvider(assistant.name());
        return response;
    }

    /**
     * 选择 assistant：优先使用请求中指定的，否则使用默认配置
     */
    private Assistant selectAssistant(String requestedProvider) {
        String name = (requestedProvider != null && !requestedProvider.isBlank())
                ? requestedProvider.toLowerCase()
                : modelProperties.getDefaultProvider().toLowerCase();

        return switch (name) {
            case "chatgpt" -> new Assistant("chatgpt", chatGptAssistant::chat, chatGptAssistant::chatStream, chatGptAssistant::chatStructured);
            case "deepseek" -> new Assistant("deepseek", deepSeekAssistant::chat, deepSeekAssistant::chatStream, deepSeekAssistant::chatStructured);
            default -> throw new IllegalArgumentException("Provider not found or not configured: " + name);
        };
    }

    /**
     * 内部辅助接口，统一 ChatGPT / DeepSeek 的调用签名
     */
    @FunctionalInterface
    private interface PlainChat {
        String chat(String message);
    }

    @FunctionalInterface
    private interface StreamChat {
        reactor.core.publisher.Flux<String> chatStream(String message);
    }

    @FunctionalInterface
    private interface StructuredChat {
        com.aidemo.chat.dto.AiReply chatStructured(String message);
    }

    /**
     * 统一封装，消除 switch 分支中的重复代码
     */
    private record Assistant(
            String name,
            PlainChat plainChat,
            StreamChat streamChat,
            StructuredChat structuredChat
    ) {
        String chat(String message) {
            return plainChat.chat(message);
        }

        reactor.core.publisher.Flux<String> chatStream(String message) {
            return streamChat.chatStream(message);
        }

        com.aidemo.chat.dto.AiReply chatStructured(String message) {
            return structuredChat.chatStructured(message);
        }
    }
}