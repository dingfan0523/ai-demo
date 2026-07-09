package com.aidemo.mcp.config;

import com.aidemo.mcp.service.McpClientRegistry;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP tool provider 配置。
 */
@Configuration
public class McpConfig {

    @Bean("mcpToolProvider")
    public ToolProvider mcpToolProvider(McpClientRegistry registry) {
        return new ToolProvider() {
            @Override
            public ToolProviderResult provideTools(ToolProviderRequest request) {
                var clients = registry.enabledClients();
                if (clients.isEmpty()) {
                    return ToolProviderResult.builder().build();
                }
                return McpToolProvider.builder()
                        .mcpClients(clients)
                        .filter((client, tool) -> registry.server(client.key())
                                .map(server -> server.allowsTool(tool.name()))
                                .orElse(false))
                        .failIfOneServerFails(false)
                        .build()
                        .provideTools(request);
            }

            @Override
            public boolean isDynamic() {
                return true;
            }
        };
    }
}
