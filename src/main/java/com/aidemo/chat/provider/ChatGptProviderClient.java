package com.aidemo.chat.provider;

import com.aidemo.chat.assistant.ChatGptAssistant;
import com.aidemo.chat.dto.AiReply;
import com.aidemo.model.config.ModelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ChatGptProviderClient implements AiProviderClient {

    private final ChatGptAssistant assistant;
    private final ModelProperties properties;

    @Override
    public String name() {
        return "chatgpt";
    }

    @Override
    public String modelName() {
        return properties.getChatgpt().getModel();
    }

    @Override
    public String chat(String message) {
        return assistant.chat(message);
    }

    @Override
    public Flux<String> chatStream(String message) {
        return assistant.chatStream(message);
    }

    @Override
    public AiReply chatStructured(String message) {
        return assistant.chatStructured(message);
    }
}
