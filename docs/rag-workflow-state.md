# RAG 开发进度留痕

## 当前状态

- 当前阶段：Iteration 7 - 持久化向量存储
- 阶段状态：TODO
- 最近更新：2026-07-30
- 工作原则：先完成可编译、可追踪、可继续的 RAG 模块边界，不直接进入 Markdown 入库或向量数据库实现；后续 RAG 开发遵循 `docs/rag-development-guidelines.md`，中文注释和中文提示优先。

## 本轮目标

- Iteration 7：持久化向量存储。
- 在保持模块契约不变的前提下，把本地内存存储替换或扩展为更真实的后端存储。
- 推荐优先考虑 PostgreSQL + pgvector。
- 增加 document/chunk/embedding 持久化和 index version/rebuild 能力。
- 查询时继续支持 metadata filter。
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
- 已完成 Iteration 4：带引用的 RAG 答案生成。
- 已新增 `DefaultContextAssembler`，把 chunk 组装为带 `[source:chunkId]` 的上下文证据块。
- 已新增 `DefaultRagAnswerService`，完成检索、上下文组装、模型调用、引用校验和低置信度兜底。
- 已新增 `POST /api/rag/query`，返回答案、可信来源、上下文、provider、model、traceId、confidence 和 trace steps。
- 已构造中文 RAG prompt，明确知识库内容是不可信参考材料。
- 已实现引用校验，模型返回的 source id 必须来自本次上下文；无效引用会从答案中移除。
- 已补充答案生成和上下文组装测试。
- 已执行 `mvn test`，结果通过：26 个测试，0 failure，0 error。
- 已完成 Iteration 5：PDF 与混合文档解析。
- 已新增 PDFBox 依赖，并排除 `commons-logging`，避免和 Spring Boot `spring-jcl` 产生 classpath 冲突提示。
- 已新增 `PdfDocumentParser`，支持文本型 PDF 抽取。
- 已支持 `sourceType=pdf` 的文件发现和入库。
- 已在 ingest 响应中新增 `diagnostics`，返回 parser、页数、文本页数、空文本页数、抽取文本长度等诊断信息。
- 已在 chunk、search source 和 RAG query context 中透传 `pageStart`、`pageEnd`。
- 已对空文本 PDF 返回中文 warning，提示可能是扫描件或图片型 PDF。
- 已补充 PDF parser 和 PDF 入库页码溯源测试。
- 已执行 `mvn test`，结果通过：29 个测试，0 failure，0 error。
- 已完成 Iteration 6：RAG 评测体系。
- 已新增 RAG 评测 DTO：`RagEvalCase`、`RagEvalRequest`、`RagEvalCaseResult`、`RagEvalResponse`。
- 已新增 `RagEvalService` 和 `DefaultRagEvalService`，复用现有检索链路执行手工黄金用例评测。
- 已新增 `POST /api/rag/evaluate`，返回 retrieval hit rate、rerank hit rate、keyword hit rate、耗时、逐用例结果和 Markdown 报告。
- 已支持通过请求体 `cases` 或 JSON 文件 `casesPath` 加载评测用例。
- 已支持通过 `reportPath` 写出 Markdown 评测报告。
- 已新增 `docs/rag-eval-guide.md`，说明评测用例格式和接口用法。
- 已补充评测服务测试，覆盖指标统计、JSON 用例加载和报告落盘。
- 已执行 `mvn test`，结果通过：31 个测试，0 failure，0 error。

## 正在进行

- 等待进入 Iteration 7。

## 待完成

- Iteration 7：持久化向量存储。
- 设计 PostgreSQL + pgvector 的最小表结构。
- 增加 document/chunk/embedding 持久化仓储实现。
- 支持 index version 和 rebuild。
- 查询时支持 metadata filter。
- 增加本地启动和迁移说明。
- 保持现有内存实现可用，便于学习阶段对比。

## 本轮暂不做

- 不接 Milvus/Qdrant 等独立向量平台。
- 不接模型化 rerank。
- 不把持久化方案扩展成分布式索引任务。
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
- 测试统计：31 tests, 0 failures, 0 errors, 0 skipped
- 时间：2026-07-30 09:02 Asia/Shanghai
