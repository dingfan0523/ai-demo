package com.aidemo.chat.provider;

import com.aidemo.chat.dto.AiReply;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;

/**
 * DeepSeek 模型提供者
 *
 * <p>DeepSeek API 兼容 OpenAI 格式，但不支持 json_schema，
 * 结构化输出使用 json_object + 格式模板实现</p>
 */
public class DeepSeekChatProvider extends AbstractChatProvider {

    /** 结构化输出的 JSON 格式模板 */
    private static final String STRUCTURED_FORMAT_TEMPLATE = """
            请按以下 JSON 格式回复，不要包含任何其他文字：
            {
              "answer": "对用户问题的核心回答",
              "keywords": ["关键词1", "关键词2"],
              "confidence": 85,
              "followUpQuestions": ["建议的后续问题1", "建议的后续问题2"],
              "category": "问题分类"
            }
            """;

    public DeepSeekChatProvider(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        super(chatModel, streamingChatModel);
    }

    @Override
    public String getName() {
        return "deepseek";
    }

    @Override
    public AiReply chatStructured(String message) {
        String userContent = message + "\n\n" + STRUCTURED_FORMAT_TEMPLATE;

        dev.langchain4j.model.chat.request.ChatRequest chatRequest =
                dev.langchain4j.model.chat.request.ChatRequest.builder()
                        .messages(UserMessage.from(userContent))
                        .responseFormat(ResponseFormat.builder()
                                .type(ResponseFormatType.JSON)
                                .build())
                        .build();

        String jsonReply = chatModel.chat(chatRequest).aiMessage().text();
        return parseAiReply(jsonReply);
    }
}
