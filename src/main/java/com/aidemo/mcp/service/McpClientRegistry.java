package com.aidemo.mcp.service;

import com.aidemo.mcp.config.McpProperties;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 懒加载并持有 MCP clients。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpClientRegistry {

    private final McpProperties properties;
    private final Map<String, McpClient> clients = new LinkedHashMap<>();

    public McpProperties properties() {
        return properties;
    }

    public synchronized List<McpClient> enabledClients() {
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }

        for (McpProperties.Server server : enabledServers()) {
            if (!clients.containsKey(server.getKey())) {
                createClient(server).ifPresent(client -> clients.put(server.getKey(), client));
            }
        }
        return new ArrayList<>(clients.values());
    }

    public synchronized Optional<McpClient> client(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return enabledClients().stream()
                .filter(client -> key.equals(client.key()))
                .findFirst();
    }

    public List<McpProperties.Server> enabledServers() {
        if (properties.getServers() == null) {
            return Collections.emptyList();
        }
        return properties.getServers().stream()
                .filter(McpProperties.Server::isEnabled)
                .filter(server -> server.getKey() != null && !server.getKey().isBlank())
                .toList();
    }

    public Optional<McpProperties.Server> server(String key) {
        return enabledServers().stream()
                .filter(server -> key.equals(server.getKey()))
                .findFirst();
    }

    private Optional<McpClient> createClient(McpProperties.Server server) {
        try {
            McpTransport transport = createTransport(server);
            McpClient client = DefaultMcpClient.builder()
                    .key(server.getKey())
                    .clientName("ai-demo")
                    .clientVersion("1.0.0")
                    .transport(transport)
                    .initializationTimeout(properties.getInitializationTimeout())
                    .toolExecutionTimeout(properties.getToolExecutionTimeout())
                    .cacheToolList(properties.isCacheToolList())
                    .build();
            log.info("MCP client initialized: key={}, transport={}", server.getKey(), server.getTransport());
            return Optional.of(client);
        } catch (RuntimeException e) {
            log.warn("MCP client initialization failed: key={}, transport={}", server.getKey(), server.getTransport(), e);
            return Optional.empty();
        }
    }

    private McpTransport createTransport(McpProperties.Server server) {
        return switch (server.getTransport()) {
            case STDIO -> StdioMcpTransport.builder()
                    .command(server.getCommand())
                    .environment(server.getEnvironment())
                    .logEvents(properties.isLogEvents())
                    .build();
            case STREAMABLE_HTTP -> StreamableHttpMcpTransport.builder()
                    .url(server.getUrl())
                    .customHeaders(server.getHeaders())
                    .timeout(properties.getInitializationTimeout())
                    .logRequests(properties.isLogRequests())
                    .logResponses(properties.isLogResponses())
                    .build();
        };
    }

    @PreDestroy
    public synchronized void close() {
        for (McpClient client : clients.values()) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Failed to close MCP client: {}", client.key(), e);
            }
        }
        clients.clear();
    }
}
