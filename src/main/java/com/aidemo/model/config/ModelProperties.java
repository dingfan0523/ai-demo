package com.aidemo.model.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 模型配置属性
 *
 * <p>所有模型都是手动 {@code @Bean} 创建，不依赖 starter 自动配置。
 * 配置集中管理在 {@code ai.*} 层级下，方便统一维护。</p>
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai")
public class ModelProperties {

    /** 默认使用的 provider 名称 */
    @NotBlank(message = "默认 provider 不能为空")
    private String defaultProvider = "deepseek";

    /** 外部 AI 请求超时时间 */
    @NotNull(message = "AI 请求超时时间不能为空")
    private Duration requestTimeout = Duration.ofSeconds(60);

    /** SSE 流式响应超时时间 */
    @NotNull(message = "SSE 流式响应超时时间不能为空")
    private Duration streamTimeout = Duration.ofMinutes(5);

    /** 同时允许占用外部 AI 调用的最大请求数 */
    @Min(value = 1, message = "最大并发请求数不能小于 1")
    @Max(value = 200, message = "最大并发请求数不能大于 200")
    private int maxConcurrentRequests = 20;

    /** 同步模型调用最大重试次数 */
    @Min(value = 0, message = "最大重试次数不能小于 0")
    @Max(value = 5, message = "最大重试次数不能大于 5")
    private int maxRetries = 2;

    /** 是否记录发送给模型的完整请求 */
    private boolean logRequests = false;

    /** 是否记录模型返回的完整响应 */
    private boolean logResponses = false;

    /** 代理配置 */
    @Valid
    private ProxyConfig proxy = new ProxyConfig();

    /** ChatGPT 配置 */
    @Valid
    private ModelConfig chatgpt = new ModelConfig();

    /** DeepSeek 配置 */
    @Valid
    private ModelConfig deepseek = new ModelConfig();

    @Data
    public static class ModelConfig {

        /** API 密钥 */
        private String apiKey;

        /** API 基础地址 */
        @NotBlank(message = "模型 API 地址不能为空")
        private String baseUrl = "https://api.openai.com/v1";

        /** 模型名称 */
        @NotBlank(message = "模型名称不能为空")
        private String model = "gpt-5.4";
    }

    @Data
    public static class ProxyConfig {

        /** 是否启用代理 */
        private boolean enabled = false;

        /** 代理主机 */
        @NotBlank(message = "代理主机不能为空")
        private String host = "127.0.0.1";

        /** 代理端口 */
        @Min(value = 1, message = "代理端口不能小于 1")
        @Max(value = 65535, message = "代理端口不能大于 65535")
        private int port = 10808;

        /** 代理类型: http 或 socks */
        @Pattern(regexp = "(?i)http|socks", message = "代理类型只支持 http 或 socks")
        private String type = "http";
    }
}
