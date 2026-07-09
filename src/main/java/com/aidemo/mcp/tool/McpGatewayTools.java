package com.aidemo.mcp.tool;

import com.aidemo.mcp.config.McpProperties;
import com.aidemo.mcp.service.McpClientRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 给模型使用的 MCP 管理与兜底调用工具。
 */
@Component
@RequiredArgsConstructor
public class McpGatewayTools {

    private final McpClientRegistry registry;
    private final ObjectMapper objectMapper;

    @Tool(name = "mcp_list_servers", value = {
            "Use when the user asks what MCP servers are configured or whether MCP capabilities are available.",
            "Returns enabled MCP server keys, transport types, and allowed tool filters."
    })
    public String listServers() {
        McpProperties properties = registry.properties();
        if (!properties.isEnabled()) {
            return "MCP 功能当前未启用。可通过 mcp.enabled=true 并配置 mcp.servers 启用。";
        }

        List<McpProperties.Server> servers = registry.enabledServers();
        if (servers.isEmpty()) {
            return "MCP 已启用，但没有可用的 MCP server 配置。";
        }

        StringBuilder result = new StringBuilder("可用 MCP servers:\n");
        for (McpProperties.Server server : servers) {
            result.append("- key=").append(server.getKey())
                    .append(", transport=").append(server.getTransport())
                    .append(", allowedTools=");
            if (server.getAllowedTools() == null || server.getAllowedTools().isEmpty()) {
                result.append("all");
            } else {
                result.append(server.getAllowedTools());
            }
            result.append('\n');
        }
        return result.toString();
    }

    @Tool(name = "mcp_list_tools", value = {
            "Use before calling MCP when you need to discover available external tools.",
            "Optionally filter by server key. Returns tool names and descriptions."
    })
    public String listTools(@P(value = "Optional MCP server key. Leave blank to list all enabled MCP servers.", required = false) String serverKey) {
        List<McpClient> clients = selectClients(serverKey);
        if (clients.isEmpty()) {
            return unavailable(serverKey);
        }

        StringBuilder result = new StringBuilder("MCP tools:\n");
        for (McpClient client : clients) {
            result.append("server=").append(client.key()).append('\n');
            for (ToolSpecification tool : client.listTools()) {
                if (registry.server(client.key()).map(server -> server.allowsTool(tool.name())).orElse(false)) {
                    result.append("- ").append(tool.name());
                    if (tool.description() != null && !tool.description().isBlank()) {
                        result.append(": ").append(tool.description());
                    }
                    result.append('\n');
                }
            }
        }
        return result.toString();
    }

    @Tool(name = "mcp_call_tool", value = {
            "Use only when an MCP tool is needed but is not directly exposed as a model tool.",
            "The arguments must be a JSON object string matching the MCP tool schema."
    })
    public String callTool(@P("MCP server key") String serverKey,
                           @P("MCP tool name") String toolName,
                           @P("JSON object string with MCP tool arguments, for example {\"query\":\"java\"}") String argumentsJson) {
        return registry.client(serverKey)
                .map(client -> executeTool(client, toolName, argumentsJson))
                .orElseGet(() -> unavailable(serverKey));
    }

    @Tool(name = "mcp_list_resources", value = {
            "Use when the user asks what resources an MCP server exposes, such as files, docs, or knowledge resources.",
            "Returns MCP resource URIs and descriptions."
    })
    public String listResources(@P("MCP server key") String serverKey) {
        return registry.client(serverKey)
                .map(client -> {
                    StringBuilder result = new StringBuilder("MCP resources for ").append(client.key()).append(":\n");
                    client.listResources().forEach(resource -> result.append("- ")
                            .append(resource.uri())
                            .append(" | name=").append(resource.name())
                            .append(" | type=").append(resource.mimeType())
                            .append(" | desc=").append(resource.description())
                            .append('\n'));
                    return result.toString();
                })
                .orElseGet(() -> unavailable(serverKey));
    }

    @Tool(name = "mcp_read_resource", value = {
            "Use when the user asks for the content of an MCP resource URI.",
            "Reads a resource from a configured MCP server."
    })
    public String readResource(@P("MCP server key") String serverKey,
                               @P("MCP resource URI") String uri) {
        return registry.client(serverKey)
                .map(client -> client.readResource(uri).toString())
                .orElseGet(() -> unavailable(serverKey));
    }

    private List<McpClient> selectClients(String serverKey) {
        if (serverKey == null || serverKey.isBlank()) {
            return registry.enabledClients();
        }
        return registry.client(serverKey).stream().toList();
    }

    private String executeTool(McpClient client, String toolName, String argumentsJson) {
        if (toolName == null || toolName.isBlank()) {
            return "MCP tool name 不能为空。";
        }
        if (registry.server(client.key()).map(server -> !server.allowsTool(toolName)).orElse(true)) {
            return "MCP tool 不在允许列表中: " + toolName;
        }
        String safeArguments = normalizeArguments(argumentsJson);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id(UUID.randomUUID().toString())
                .name(toolName)
                .arguments(safeArguments)
                .build();
        ToolExecutionResult result = client.executeTool(request);
        if (result.resultText() != null) {
            return result.resultText();
        }
        if (result.result() != null) {
            return result.result().toString();
        }
        return result.toString();
    }

    private String normalizeArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return "{}";
        }
        try {
            objectMapper.readTree(argumentsJson);
            return argumentsJson;
        } catch (Exception e) {
            throw new IllegalArgumentException("MCP tool arguments 必须是 JSON object 字符串", e);
        }
    }

    private String unavailable(String serverKey) {
        if (!registry.properties().isEnabled()) {
            return "MCP 功能当前未启用。";
        }
        if (serverKey == null || serverKey.isBlank()) {
            return "没有可用的 MCP server。";
        }
        return "未找到可用的 MCP server: " + serverKey;
    }
}
