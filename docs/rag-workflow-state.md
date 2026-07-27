# RAG 开发进度留痕

## 当前状态

- 当前阶段：Iteration 2 - 基础 Embedding 与本地向量存储
- 阶段状态：TODO
- 最近更新：2026-07-27
- 工作原则：先完成可编译、可追踪、可继续的 RAG 模块边界，不直接进入 Markdown 入库或向量数据库实现；后续 RAG 开发遵循 `docs/rag-development-guidelines.md`，中文注释和中文提示优先。

## 本轮目标

- Iteration 2：基础 Embedding 与本地向量存储。
- 新增 `EmbeddingService` 的最小可运行实现或可替换适配器。
- 新增本地 `VectorStore` 实现。
- 提供 `POST /api/rag/search` 检索入口。
- 补充向量检索和 topK/minScore 测试。
- 保持现有 `/api/chat` 行为不变。

## 已完成

- 已确认 RAG 总体设计方向。
- 已新增 `docs/rag-todotask.md`，记录后续迭代路线。
- 已完成 Iteration 0：模块骨架与契约。
- 已新增 `com.aidemo.rag` 包边界。
- 已新增 RAG 配置、DTO、领域模型、仓储接口、服务接口、向量存储接口和安全接口。
- 已新增基础测试：`RagPropertiesTest`、`RagModuleBoundaryTest`。
- 已执行 `mvn test`，结果通过：14 个测试，0 failure，0 error。
- 已新增 `docs/rag-development-guidelines.md`，沉淀中文注释、中文校验提示和 RAG prompt 规范。
- 已将当前 RAG 骨架中的校验 message 调整为中文。
- 已为 RAG DTO、领域模型、仓储接口、服务接口、向量接口和安全接口补充中文类/方法说明。
- 已再次执行 `mvn test`，结果通过：14 个测试，0 failure，0 error。
- 已完成 Iteration 1：Markdown 入库与可观察切分。
- 已新增 `POST /api/rag/ingest`。
- 已新增 Markdown 解析、清洗、切分、内存仓储和入库编排。
- 已实现基于 `contentHash` 的重复入库跳过。
- 已补充 Markdown 入库相关测试。
- 已执行 `mvn test`，结果通过：17 个测试，0 failure，0 error。

## 正在进行

- 等待进入 Iteration 2。

## 待完成

- Iteration 2：基础 Embedding 与本地向量存储。
- 设计并实现学习阶段可运行的 embedding 方案。
- 保存 chunk id、embedding model、vector dimension、index version。
- 提供 `POST /api/rag/search`，用于只检索、不生成答案。
- 补充检索响应和边界参数测试。

## 本轮暂不做

- 不接外部向量数据库。
- 不实现 `/api/rag/query`。
- 不修改 `src/main/resources/application.yml` 中已有本地配置。

## 切换会话恢复提示

如果切换会话后继续，请先阅读：

1. `docs/rag-todotask.md`
2. `docs/rag-workflow-state.md`
3. `docs/rag-development-guidelines.md`
4. `git status --short`

然后从“当前状态”和“待完成”继续，不要重复设计已经确认的 RAG 总体方案。

## 本轮验证记录

- 命令：`mvn test`
- 结果：BUILD SUCCESS
- 测试统计：17 tests, 0 failures, 0 errors, 0 skipped
- 时间：2026-07-27 15:56 Asia/Shanghai
