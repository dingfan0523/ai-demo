# MCP 配置说明

本项目已支持把外部 MCP server 暴露的工具接入到 LangChain4j `@AiService`。

默认 `mcp.enabled=false`，不会连接任何外部 MCP server。需要使用时，可在本地配置中增加类似内容：

```yaml
mcp:
  enabled: true
  initialization-timeout: 10s
  tool-execution-timeout: 30s
  cache-tool-list: true
  servers:
    - key: local-docs
      enabled: true
      transport: stdio
      command:
        - npx
        - -y
        - "@modelcontextprotocol/server-filesystem"
        - "D:/work/my/ai-demo/docs"
      allowed-tools:
        - list_directory
        - read_file
    - key: docs-api
      enabled: false
      transport: streamable-http
      url: http://localhost:3001/mcp
      allowed-tools:
        - search_docs
        - read_doc
```

已接入能力：

- 动态 MCP tools：配置的 MCP server 工具会通过 `mcpToolProvider` 暴露给大模型。
- `mcp_list_servers`：查看当前 MCP server 配置。
- `mcp_list_tools`：查看某个或全部 MCP server 暴露的工具。
- `mcp_call_tool`：兜底调用指定 MCP tool。
- `mcp_list_resources`：查看 MCP resources。
- `mcp_read_resource`：读取指定 MCP resource。

建议先只开放只读类 MCP tool，例如文档搜索、文件读取、API 文档查询。写文件、执行命令、访问生产系统等动作类 tool 应增加鉴权、审计和白名单。
