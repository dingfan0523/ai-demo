package com.aidemo.chat.service;

import com.aidemo.chat.dto.AiReply;
import com.aidemo.chat.dto.ChatRequest;
import com.aidemo.chat.dto.ChatResponse;
import com.aidemo.chat.provider.AiProviderClient;
import com.aidemo.model.config.ModelProperties;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatServiceTest {

    @Test
    void chatPlainUsesDefaultProviderWhenRequestProviderIsBlank() {
        ModelProperties properties = properties("deepseek");
        ChatService service = new ChatService(List.of(provider("deepseek", "deepseek-test")), properties);
        service.afterPropertiesSet();

        ChatRequest request = new ChatRequest();
        request.setMessage("hello");

        assertThat(service.chatPlain(request)).isEqualTo("deepseek-test:hello");
    }

    @Test
    void chatStructuredReturnsActualProviderAndModel() {
        ModelProperties properties = properties("deepseek");
        ChatService service = new ChatService(List.of(provider("deepseek", "deepseek-test")), properties);
        service.afterPropertiesSet();

        ChatRequest request = new ChatRequest();
        request.setMessage("hello");

        ChatResponse response = service.chatStructured(request);

        assertThat(response.getProvider()).isEqualTo("deepseek");
        assertThat(response.getModel()).isEqualTo("deepseek-test");
        assertThat(response.getReply().getAnswer()).isEqualTo("deepseek-test:hello");
    }

    @Test
    void unknownProviderFailsFast() {
        ModelProperties properties = properties("deepseek");
        ChatService service = new ChatService(List.of(provider("deepseek", "deepseek-test")), properties);
        service.afterPropertiesSet();

        ChatRequest request = new ChatRequest();
        request.setMessage("hello");
        request.setProvider("missing");

        assertThatThrownBy(() -> service.chatPlain(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider not found");
    }

    @Test
    void invalidDefaultProviderFailsDuringInitialization() {
        ModelProperties properties = properties("missing");
        ChatService service = new ChatService(List.of(provider("deepseek", "deepseek-test")), properties);

        assertThatThrownBy(service::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Default AI provider is not registered");
    }

    private ModelProperties properties(String defaultProvider) {
        ModelProperties properties = new ModelProperties();
        properties.setDefaultProvider(defaultProvider);
        properties.setMaxConcurrentRequests(2);
        return properties;
    }

    private AiProviderClient provider(String name, String modelName) {
        return new AiProviderClient() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String modelName() {
                return modelName;
            }

            @Override
            public String chat(String message) {
                return modelName + ":" + message;
            }

            @Override
            public Flux<String> chatStream(String message) {
                return Flux.just(modelName, message);
            }

            @Override
            public AiReply chatStructured(String message) {
                AiReply reply = new AiReply();
                reply.setAnswer(modelName + ":" + message);
                return reply;
            }
        };
    }
}
