package com.aidemo.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型配置属性
 *
 * <p>所有模型都是手动 {@code @Bean} 创建，不依赖 starter 自动配置。
 * 配置集中管理在 {@code ai.*} 层级下，方便统一维护。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class ModelProperties {

    /** 默认使用的 provider 名称 */
    private String defaultProvider = "deepseek";

    /** 代理配置 */
    private ProxyConfig proxy = new ProxyConfig();

    /** ChatGPT 配置 */
    private ModelConfig chatgpt = new ModelConfig();

    /** DeepSeek 配置 */
    private ModelConfig deepseek = new ModelConfig();

    @Data
    public static class ModelConfig {

        /** API 密钥 */
        private String apiKey;

        /** API 基础地址 */
        private String baseUrl = "https://api.openai.com/v1";

        /** 模型名称 */
        private String model = "gpt-5.4";
    }

    @Data
    public static class ProxyConfig {

        /** 是否启用代理 */
        private boolean enabled = false;

        /** 代理主机 */
        private String host = "127.0.0.1";

        /** 代理端口 */
        private int port = 10808;

        /** 代理类型: http 或 socks */
        private String type = "http";
    }
}