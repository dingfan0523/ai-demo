# RAG 开发进度留痕

## 当前状态

- 当前阶段：Iteration 4 - 带引用的 RAG 答案生成
- 阶段状态：TODO
- 最近更新：2026-07-29
- 工作原则：先完成可编译、可追踪、可继续的 RAG 模块边界，不直接进入 Markdown 入库或向量数据库实现；后续 RAG 开发遵循 `docs/rag-development-guidelines.md`，中文注释和中文提示优先。

## 本轮目标

- Iteration 4：带引用的 RAG 答案生成。
- 新增 `POST /api/rag/query`，在现有检索链路基础上生成答案。
- 构造中文 RAG prompt，明确知识库内容只是“不可信参考材料”。
- 返回 `answer`、`sources`、`contexts`、`provider`、`model`、`traceId`、`confidence`。
- 增加引用校验，避免模型编造不存在的 source id。
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
- 已完成 Iteration 2：基础 Embedding 与本地向量存储。
- 已新增 `LocalHashEmbeddingService`，使用本地 hash embedding 跑通学习链路。
- 已新增 `InMemoryVectorStore`，支持 topK、minScore 和 tags 过滤。
- 已在入库后为 chunk 生成并保存向量。
- 已新增 `POST /api/rag/search`，返回 topK、vector score、source、contentPreview 和 trace steps。
- 已补充 embedding、向量存储和检索服务测试。
- 已执行 `mvn test`，结果通过：20 个测试，0 failure，0 error。
- 已完成 Iteration 3：混合召回与可解释 Rerank。
- 已新增 `RagTextTokenizer`，支持英文标识符、数字、中文单字和中文 bigram token。
- 已新增 `KeywordScoringService` 和 `KeywordScore`，返回关键词得分和命中的 token。
- 已新增 `ExplainableRerankService`，按 vector score、keyword score、标题命中和元数据命中合成 `rerankScore`。
- 已将 `DefaultRetrievalService` 调整为混合召回，合并向量候选和关键词候选后再执行 rerank。
- search 响应已返回 `vectorScore`、`keywordScore`、`rerankScore`、`matchedTokens` 和可观察 trace steps。
- 已补充混合召回与 rerank 相关测试。
- 已执行 `mvn test`，结果通过：23 个测试，0 failure，0 error。

## 正在进行

- 等待进入 Iteration 4。

## 待完成

- Iteration 4：带引用的 RAG 答案生成。
- 设计 RAG answer adapter，避免把检索逻辑塞进现有 `ChatService`。
- 组装带 source id 的上下文片段。
- 构造中文 RAG prompt，并要求模型只基于上下文回答。
- 增加引用校验和低置信度兜底回答。
- 补充答案生成、引用映射和空召回测试。

## 本轮暂不做

- 不接外部向量数据库。
- 不接模型化 rerank。
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
- 测试统计：23 tests, 0 failures, 0 errors, 0 skipped
- 时间：2026-07-29 09:15 Asia/Shanghai
