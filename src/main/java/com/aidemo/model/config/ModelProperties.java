package com.aidemo.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 模型配置属性
 *
 * <p>从 application.yml 的 ai 节点加载配置，支持多个模型提供者</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class ModelProperties {

    /** 默认使用的模型提供者 */
    private String defaultProvider = "deepseek";

    /** 各模型配置映射，key 为 provider 名称 */
    private Map<String, ModelConfig> models = new HashMap<>();

    /** 代理配置 */
    private ProxyConfig proxy = new ProxyConfig();

    @Data
    public static class ModelConfig {

        /** API 密钥 */
        private String apiKey;

        /** API 基础地址 */
        private String baseUrl;

        /** 模型名称 */
        private String model;
    }

    /** 代理配置 */
    @Data
    public static class ProxyConfig {

        /** 是否启用代理 */
        private boolean enabled = false;

        /** 代理主机 */
        private String host = "127.0.0.1";

        /** 代理端口 */
        private int port = 10808;

        /** 代理类型: http 或 socks */
        private String type = "socks";
    }
}
