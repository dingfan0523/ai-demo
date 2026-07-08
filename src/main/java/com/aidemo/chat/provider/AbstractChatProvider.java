package com.aidemo.chat.provider;

import com.aidemo.chat.core.ChatProvider;
import com.aidemo.chat.dto.AiReply;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.function.Consumer;

/**
 * AI 模型提供者抽象基类
 *
 * <p>封装普通问答和流式问答的通用实现，子类只需覆盖结构化问答的差异逻辑。
 * 使用模板方法模式，减少重复代码。</p>
 */
public abstract class AbstractChatProvider implements ChatProvider {

    protected final ChatModel chatModel;
    protected final StreamingChatModel streamingChatModel;
    protected final ObjectMapper objectMapper;

    public AbstractChatProvider(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String chatPlain(String message) {
        return chatModel.chat(message);
    }

    @Override
    public void chatStream(String message,
                           Consumer<String> onToken,
                           Runnable onComplete,
                           Consumer<Throwable> onError) {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from(message))
                .build();

        streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                onToken.accept(token);
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }

    /**
     * 将 AI 返回的 JSON 字符串解析为 AiReply 实体
     */
    protected AiReply parseAiReply(String json) {
        try {
            return objectMapper.readValue(json.trim(), AiReply.class);
        } catch (Exception e) {
            // 降级处理：把原始文本作为 answer 返回
            AiReply fallback = new AiReply();
            fallback.setAnswer(json);
            fallback.setConfidence(0);
            return fallback;
        }
    }
}
