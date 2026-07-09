package com.aidemo.model.config;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
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
import java.util.List;

/**
 * AI 模型 Bean 配置
 *
 * <p>手动创建所有模型 Bean，不依赖 starter 自动配置。
 * 主要通过 {@link OpenAiChatModel} 和 {@link OpenAiStreamingChatModel} 构建，
 * 支持统一代理配置。</p>
 *
 * <h3>两种方案对比</h3>
 * <pre>
 * ┌─────────────────────┬──────────────────────────────────┬──────────────────────────────────────┐
 * │                     │ 方案 A：Starter 自动配置           │ 方案 B：纯手工 @Bean ✅               │
 * ├─────────────────────┼──────────────────────────────────┼──────────────────────────────────────┤
 * │ ChatGPT 配置        │ application.yml + 零代码         │ 需要手写 OpenAiChatModel.builder()   │
 * │                     │ （但需 Spring Boot 3.3+）        │ （兼容 Spring Boot 3.2）              │
 * │ DeepSeek 配置       │ 需要手动 @Bean                   │ 全量手动 @Bean                       │
 * │ 代理配置            │ RestClient.Builder 自定义         │ JdkHttpClientBuilder 代理            │
 * │ 扩展新模型          │ 加 @Bean + @AiService            │ 加 @Bean + @AiService                │
 * └─────────────────────┴──────────────────────────────────┴──────────────────────────────────────┘
 * </pre>
 */
@Slf4j
@Configuration
public class ModelConfig {

    /**
     * ChatGPT 同步聊天模型 Bean
     *
     * <p>Bean 名称 {@code openAiChatModel} 与 {@link com.aidemo.chat.assistant.ChatGptAssistant}
     * 中的 {@code @AiService(chatModel = "openAiChatModel")} 对应。</p>
     */
    @Bean
    public OpenAiChatModel openAiChatModel(ModelProperties properties) {
        var config = properties.getChatgpt();
        return buildChatModel("openAiChatModel", config, properties);
    }

    /**
     * ChatGPT 流式聊天模型 Bean
     */
    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel(ModelProperties properties) {
        var config = properties.getChatgpt();
        return buildStreamingChatModel("openAiStreamingChatModel", config, properties);
    }

    /**
     * DeepSeek 同步聊天模型 Bean
     */
    @Bean
    public OpenAiChatModel deepSeekChatModel(ModelProperties properties) {
        var config = properties.getDeepseek();
        return buildChatModel("deepSeekChatModel", config, properties);
    }

    /**
     * DeepSeek 流式聊天模型 Bean
     */
    @Bean
    public OpenAiStreamingChatModel deepSeekStreamingChatModel(ModelProperties properties) {
        var config = properties.getDeepseek();
        return buildStreamingChatModel("deepSeekStreamingChatModel", config, properties);
    }

    private OpenAiChatModel buildChatModel(String beanName, ModelProperties.ModelConfig config, ModelProperties properties) {
        ModelProperties.ProxyConfig proxy = properties.getProxy();
        var builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModel())
                .timeout(properties.getRequestTimeout())
                .maxRetries(properties.getMaxRetries())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses());

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Model [{}] API key not configured, will fail at runtime", beanName);
        }

        if (proxy.isEnabled()) {
            builder.httpClientBuilder(createProxyHttpClientBuilder(proxy));
        }

        OpenAiChatModel model = builder.build();
        log.info("Model [{}] initialized: baseUrl={}, model={}", beanName, config.getBaseUrl(), config.getModel());
        if (proxy.isEnabled()) {
            log.info("  proxy: {}://{}:{}", proxy.getType(), proxy.getHost(), proxy.getPort());
        }
        return model;
    }

    private OpenAiStreamingChatModel buildStreamingChatModel(String beanName, ModelProperties.ModelConfig config, ModelProperties properties) {
        ModelProperties.ProxyConfig proxy = properties.getProxy();
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModel())
                .timeout(properties.getRequestTimeout())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses());

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Streaming model [{}] API key not configured, will fail at runtime", beanName);
        }

        if (proxy.isEnabled()) {
            builder.httpClientBuilder(createProxyHttpClientBuilder(proxy));
        }

        return builder.build();
    }

    /**
     * 创建带代理的 HttpClientBuilder
     *
     * <p>使用 {@link JdkHttpClientBuilder} 包装 {@link HttpClient.Builder}，
     * 通过 {@link ProxySelector} 配置 SOCKS 或 HTTP 代理。</p>
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
