package com.aidemo.mcp.tool;

import com.aidemo.mcp.config.McpProperties;
import com.aidemo.mcp.service.McpClientRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McpGatewayToolsTest {

    @Test
    void listServersReturnsDisabledMessageWhenMcpIsDisabled() {
        McpGatewayTools tools = tools(new McpProperties());

        assertThat(tools.listServers()).contains("MCP 功能当前未启用");
    }

    @Test
    void listServersReturnsConfiguredServerSummary() {
        McpProperties properties = new McpProperties();
        properties.setEnabled(true);
        McpProperties.Server server = new McpProperties.Server();
        server.setKey("docs");
        server.setTransport(McpProperties.TransportType.STREAMABLE_HTTP);
        server.setUrl("http://localhost:3001/mcp");
        server.setAllowedTools(List.of("search_docs", "read_doc"));
        properties.setServers(List.of(server));

        String result = tools(properties).listServers();

        assertThat(result)
                .contains("key=docs")
                .contains("STREAMABLE_HTTP")
                .contains("search_docs")
                .contains("read_doc");
    }

    @Test
    void listToolsDoesNotTryToConnectWhenMcpIsDisabled() {
        McpGatewayTools tools = tools(new McpProperties());

        assertThat(tools.listTools(null)).contains("MCP 功能当前未启用");
    }

    private McpGatewayTools tools(McpProperties properties) {
        return new McpGatewayTools(new McpClientRegistry(properties), new ObjectMapper());
    }
}
