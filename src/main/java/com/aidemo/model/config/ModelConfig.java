package com.aidemo.model.config;

import com.aidemo.chat.core.ChatProvider;
import com.aidemo.chat.provider.DeepSeekChatProvider;
import com.aidemo.chat.provider.OpenAiChatProvider;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型配置类
 *
 * <p>为每个已配置的模型创建 ChatModel + StreamingChatModel，
 * 并根据 provider 名称选择合适的 ChatProvider 策略实现</p>
 */
@Slf4j
@Configuration
public class ModelConfig {

    @Bean
    public Map<String, ChatProvider> chatProviders(ModelProperties modelProperties) {
        Map<String, ChatProvider> providers = new HashMap<>();

        modelProperties.getModels().forEach((name, config) -> {
            if (config.getApiKey() == null || config.getApiKey().isBlank()
                    || config.getApiKey().startsWith("your-")) {
                log.warn("Model [{}] API key not configured, skipping", name);
                return;
            }

            var chatBuilder = OpenAiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModel())
                    .logRequests(true)
                    .logResponses(true);

            var streamingBuilder = OpenAiStreamingChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModel())
                    .logRequests(true)
                    .logResponses(true);

            if (modelProperties.getProxy().isEnabled()) {
                HttpClientBuilder proxyClientBuilder = createProxyHttpClientBuilder(modelProperties.getProxy());
                chatBuilder.httpClientBuilder(proxyClientBuilder);
                streamingBuilder.httpClientBuilder(proxyClientBuilder);
                log.info("Model [{}] using proxy: {}://{}:{}", name,
                        modelProperties.getProxy().getType(),
                        modelProperties.getProxy().getHost(),
                        modelProperties.getProxy().getPort());
            }

            ChatModel chatModel = chatBuilder.build();
            StreamingChatModel streamingChatModel = streamingBuilder.build();

            ChatProvider provider = createProvider(name, chatModel, streamingChatModel);
            providers.put(name, provider);
            log.info("Provider [{}] initialized: baseUrl={}, model={}", name, config.getBaseUrl(), config.getModel());
        });

        if (providers.isEmpty()) {
            log.warn("No model configured. Please set valid API keys.");
        }

        return providers;
    }

    /**
     * 根据 provider 名称创建对应的策略实现
     *
     * <p>新增模型时，在此方法中添加对应分支即可</p>
     */
    private ChatProvider createProvider(String name, ChatModel chatModel, StreamingChatModel streamingChatModel) {
        return switch (name.toLowerCase()) {
            case "chatgpt" -> new OpenAiChatProvider(chatModel, streamingChatModel);
            case "deepseek" -> new DeepSeekChatProvider(chatModel, streamingChatModel);
            // 新增模型时在此添加
            default -> {
                log.warn("Unknown provider [{}], fallback to DeepSeek-style (json_object)", name);
                yield new DeepSeekChatProvider(chatModel, streamingChatModel);
            }
        };
    }

    /**
     * 创建带代理的 HttpClientBuilder
     */
    private HttpClientBuilder createProxyHttpClientBuilder(ModelProperties.ProxyConfig proxy) {
        ProxySelector proxySelector;

        if ("socks".equalsIgnoreCase(proxy.getType())) {
            proxySelector = new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(new Proxy(Proxy.Type.SOCKS,
                            new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException e) {
                    // no-op
                }
            };
        } else {
            proxySelector = ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        }

        HttpClient.Builder jdkBuilder = HttpClient.newBuilder().proxy(proxySelector);
        return new JdkHttpClientBuilder().httpClientBuilder(jdkBuilder);
    }
}
