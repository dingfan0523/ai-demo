package com.aidemo.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP client 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    /** 是否启用 MCP 接入；默认关闭，避免未配置 server 时影响启动。 */
    private boolean enabled = false;

    /** MCP client 初始化超时时间。 */
    private Duration initializationTimeout = Duration.ofSeconds(10);

    /** MCP tool 执行超时时间。 */
    private Duration toolExecutionTimeout = Duration.ofSeconds(30);

    /** 是否缓存 MCP server 的工具列表。 */
    private boolean cacheToolList = true;

    /** 是否记录 MCP 请求日志。 */
    private boolean logRequests = false;

    /** 是否记录 MCP 响应日志。 */
    private boolean logResponses = false;

    /** 是否记录 stdio MCP 事件日志。 */
    private boolean logEvents = false;

    /** 已配置的 MCP server。 */
    private List<Server> servers = new ArrayList<>();

    @Data
    public static class Server {

        /** server 是否启用。 */
        private boolean enabled = true;

        /** server 唯一标识，用于日志、过滤和手动调用。 */
        private String key;

        /** 传输方式。 */
        private TransportType transport = TransportType.STDIO;

        /** stdio 启动命令，例如 ["npx", "-y", "@modelcontextprotocol/server-filesystem", "."]。 */
        private List<String> command = new ArrayList<>();

        /** streamable-http 的 /mcp URL。 */
        private String url;

        /** stdio 环境变量。 */
        private Map<String, String> environment = new LinkedHashMap<>();

        /** HTTP 请求头。 */
        private Map<String, String> headers = new LinkedHashMap<>();

        /** 允许暴露给模型的工具名；为空表示不做工具名过滤。 */
        private List<String> allowedTools = new ArrayList<>();

        public boolean allowsTool(String toolName) {
            return allowedTools == null || allowedTools.isEmpty() || allowedTools.contains(toolName);
        }
    }

    public enum TransportType {
        STDIO,
        STREAMABLE_HTTP
    }
}
