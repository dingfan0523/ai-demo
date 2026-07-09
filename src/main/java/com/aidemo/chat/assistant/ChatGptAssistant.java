package com.aidemo.chat.assistant;

import com.aidemo.chat.dto.AiReply;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * ChatGPT {@code @AiService} 声明式接口
 *
 * <p>由 langchain4j-spring-boot-starter 自动扫描并创建代理实现，
 * 底层绑定 {@code openAiChatModel} Bean（由 langchain4j-open-ai-spring-boot-starter 自动配置）。
 * 无需手动创建 ChatModel、无需处理消息转换。</p>
 *
 * <h3>Bean 绑定说明</h3>
 * <ul>
 *   <li>{@code chatModel = "openAiChatModel"} — 自动配置的 ChatGPT 同步模型</li>
 *   <li>{@code streamingChatModel = "openAiStreamingChatModel"} — 自动配置的流式模型</li>
 * </ul>
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        tools = "engineeringKnowledgeTools"
)
public interface ChatGptAssistant {

    /**
     * 普通文本问答（同步）
     */
    @SystemMessage("""
            You are a helpful assistant focused on Java backend, AI agent, and LLM engineering knowledge.
            When the user's question would benefit from a Java, agent, LLM engineering, or knowledge routing tool,
            call the most relevant tool before answering. Do not call tools for casual chat or questions unrelated to these topics.
            """)
    String chat(@UserMessage String message);

    /**
     * 流式问答（返回 Flux，由 ChatService 转换为 SSE）
     */
    @SystemMessage("""
            You are a helpful assistant focused on Java backend, AI agent, and LLM engineering knowledge.
            When the user's question would benefit from a Java, agent, LLM engineering, or knowledge routing tool,
            call the most relevant tool before answering. Do not call tools for casual chat or questions unrelated to these topics.
            """)
    Flux<String> chatStream(@UserMessage String message);

    /**
     * 结构化问答（返回 POJO，自动 JSON 模式）
     */
    @SystemMessage("""
            You are a helpful assistant focused on Java backend, AI agent, and LLM engineering knowledge.
            When the user's question would benefit from a Java, agent, LLM engineering, or knowledge routing tool,
            call the most relevant tool before answering. Do not call tools for casual chat or questions unrelated to these topics.
            Always respond in valid JSON format.
            Your response must be a JSON object with the following fields:
            - "answer": a string, the core answer to the user's question
            - "keywords": an array of strings, relevant keywords
            - "confidence": an integer 0-100, confidence level
            - "followUpQuestions": an array of strings, suggested follow-up questions
            - "category": a string, category of the question
            """)
    AiReply chatStructured(@UserMessage String message);
}
