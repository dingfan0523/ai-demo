package com.aidemo.chat.provider;

import com.aidemo.chat.dto.AiReply;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * OpenAI / ChatGPT 模型提供者
 *
 * <p>支持 json_schema 结构化输出，可靠性最高</p>
 */
public class OpenAiChatProvider extends AbstractChatProvider {

    /** AiReply 对应的 JSON Schema */
    private static final JsonSchema AI_REPLY_SCHEMA = JsonSchema.builder()
            .name("AiReply")
            .rootElement(JsonObjectSchema.builder()
                    .addStringProperty("answer", "对用户问题的核心回答")
                    .addProperty("keywords", JsonArraySchema.builder()
                            .items(JsonStringSchema.builder().build())
                            .description("关键词列表")
                            .build())
                    .addIntegerProperty("confidence", "置信度 0-100")
                    .addProperty("followUpQuestions", JsonArraySchema.builder()
                            .items(JsonStringSchema.builder().build())
                            .description("建议后续问题")
                            .build())
                    .addStringProperty("category", "问题分类")
                    .required("answer", "keywords", "confidence", "followUpQuestions", "category")
                    .build())
            .build();

    public OpenAiChatProvider(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        super(chatModel, streamingChatModel);
    }

    @Override
    public String getName() {
        return "chatgpt";
    }

    @Override
    public AiReply chatStructured(String message) {
        dev.langchain4j.model.chat.request.ChatRequest chatRequest =
                dev.langchain4j.model.chat.request.ChatRequest.builder()
                        .messages(UserMessage.from(message))
                        .responseFormat(ResponseFormat.builder()
                                .type(ResponseFormatType.JSON)
                                .jsonSchema(AI_REPLY_SCHEMA)
                                .build())
                        .build();

        String jsonReply = chatModel.chat(chatRequest).aiMessage().text();
        return parseAiReply(jsonReply);
    }
}
