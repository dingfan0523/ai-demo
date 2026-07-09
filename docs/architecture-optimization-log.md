# AI Demo 后端架构优化记录

## 本次优化背景

- 日期：2026-07-08
- 角色视角：`.codex/agents/engineering-backend-architect.toml`
- 范围：后端架构、可靠性、安全日志、可扩展性、配置校验、基础可观测性、测试覆盖。
- 明确排除：`application.yml` 中的 `api-key` 暂不处理。

## 优化点与原因

### 1. Provider 选择逻辑从 switch 改为可注册 client

- 涉及文件：
  - `src/main/java/com/aidemo/chat/provider/AiProviderClient.java`
  - `src/main/java/com/aidemo/chat/provider/ChatGptProviderClient.java`
  - `src/main/java/com/aidemo/chat/provider/DeepSeekProviderClient.java`
  - `src/main/java/com/aidemo/chat/service/ChatService.java`
- 优化原因：原逻辑把 provider 选择硬编码在 `ChatService` 的 switch 中，后续新增模型时必须修改核心业务类。
- 优化效果：新增 provider 时只需新增一个 `AiProviderClient` Bean，`ChatService` 自动通过注册表选择。

### 2. 结构化响应补齐实际模型名

- 涉及文件：`src/main/java/com/aidemo/chat/service/ChatService.java`
- 优化原因：`ChatResponse` 已定义 `model` 字段，但之前没有赋值，不利于排查问题和审计请求。
- 优化效果：结构化接口会返回实际 provider 和 model，便于定位模型差异导致的行为变化。

### 3. SSE 流式响应增加超时、取消订阅和清理保护

- 涉及文件：`src/main/java/com/aidemo/chat/service/ChatService.java`
- 优化原因：原先 `SseEmitter(0L)` 永不超时，且客户端断开后没有明确取消上游 Flux 订阅，长期运行有资源泄漏风险。
- 优化效果：SSE 使用 `ai.stream-timeout`，并在完成、超时、错误时释放并发许可和取消订阅；同时通过一次性清理保护避免重复释放。

### 4. 外部 AI 调用增加超时、重试和并发保护

- 涉及文件：
  - `src/main/java/com/aidemo/model/config/ModelConfig.java`
  - `src/main/java/com/aidemo/model/config/ModelProperties.java`
  - `src/main/resources/application.yml`
  - `src/main/java/com/aidemo/chat/service/ChatService.java`
- 优化原因：外部 AI 服务慢、失败或突发并发过高时，容易拖住 Web 请求线程并放大故障。
- 优化效果：
  - `ai.request-timeout` 控制模型请求等待时间。
  - `ai.max-retries` 控制同步调用重试次数。
  - `ai.max-concurrent-requests` 控制本地同时占用外部 AI 调用的请求数。
  - 超过并发上限时返回 429。

### 5. 请求/响应完整日志改为默认关闭

- 涉及文件：
  - `src/main/java/com/aidemo/model/config/ModelConfig.java`
  - `src/main/java/com/aidemo/model/config/ModelProperties.java`
  - `src/main/resources/application.yml`
- 优化原因：AI prompt 和 response 可能包含用户输入、隐私或业务敏感信息，默认记录完整内容有安全与合规风险。
- 优化效果：通过 `ai.log-requests` 和 `ai.log-responses` 控制，默认关闭。

### 6. 配置属性增加启动期校验

- 涉及文件：`src/main/java/com/aidemo/model/config/ModelProperties.java`
- 优化原因：代理端口、代理类型、模型名、base-url、并发上限等配置如果错误，应该启动时暴露，而不是等请求进入后才失败。
- 优化效果：使用 `@Validated` 和 Jakarta Validation 约束配置字段。

### 7. Controller 异常处理集中到全局处理器

- 涉及文件：
  - `src/main/java/com/aidemo/chat/controller/ChatController.java`
  - `src/main/java/com/aidemo/exception/GlobalExceptionHandler.java`
  - `src/main/java/com/aidemo/exception/AiServiceBusyException.java`
- 优化原因：原 Controller 内部 try/catch 重复，业务错误可能以 HTTP 200 返回，状态码和响应体不一致。
- 优化效果：统一把参数错误转 400、并发繁忙转 429、服务状态错误转 503、未知异常转 500。

### 8. 增加基础健康检查入口

- 涉及文件：
  - `pom.xml`
  - `src/main/resources/application.yml`
- 优化原因：后端服务需要最基本的可观测入口，便于部署环境判断服务是否存活。
- 优化效果：引入 Actuator，并暴露 `health`、`info`，启用 health probes。

### 9. 增加基础单元测试

- 涉及文件：
  - `pom.xml`
  - `src/test/java/com/aidemo/chat/service/ChatServiceTest.java`
- 优化原因：provider 选择、默认 provider、未知 provider、响应字段回填属于核心行为，后续重构容易回归。
- 优化效果：为核心服务行为建立基础测试网。

## 需要留痕的设计取舍

- 没有引入 Resilience4j：当前项目仍是 demo 体量，本次优先使用 LangChain4j builder 自带 timeout/retry 和本地 Semaphore 控制并发，减少依赖复杂度。
- 流式模型未配置重试：SSE 已开始向客户端输出后重试会造成重复 token 或语义不连续，因此本次只对同步模型配置 `maxRetries`。
- `api-key` 未处理：按本次要求暂不调整密钥管理方式。
- 没有增加认证鉴权：当前接口看起来仍是本地 demo 形态；如果要对公网或多人使用，应另起一轮做 API 认证、限流和审计。

## 验证记录

- 已执行：`mvn clean test`
- 结果：构建成功，17 个主源码文件重新编译，5 个测试通过，其中包含 1 个 Spring 上下文启动测试。

## 后续建议

- 增加接口级限流，例如基于 IP、用户或 API token。
- 增加调用指标，例如 provider、model、耗时、失败率、并发拒绝次数。
- 为 SSE 增加集成测试，覆盖客户端断开、上游错误、超时完成。
- 为生产环境增加独立 profile，关闭 Swagger 或加访问控制。
- 后续如果知识库内容增长，应把当前内置 tool 返回的静态知识卡片迁移为 RAG 检索：先建立文档目录、标签体系和评测问题集，再接向量库或重排模型。
