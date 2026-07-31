# RAG 开发进度留痕

## 当前状态

- 当前阶段：Iteration 8 - 高级检索实验
- 阶段状态：TODO
- 最近更新：2026-07-30
- 工作原则：先完成可编译、可追踪、可继续的 RAG 模块边界，不直接进入 Markdown 入库或向量数据库实现；后续 RAG 开发遵循 `docs/rag-development-guidelines.md`，中文注释和中文提示优先。

## 本轮目标

- Iteration 8：高级检索实验。
- 在 baseline 可评测后，尝试更成熟的 RAG 优化能力。
- 候选实验包括 LLM query rewrite、multi-query expansion、术语同义词词典、模型化 rerank、parent-child retrieval。
- 每个实验都应有 eval 前后对比结果。
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
- 已完成 Iteration 7：Elasticsearch 7.17 兼容持久化向量存储。
- 已新增 `rag.vector-store.type` 配置，默认继续使用 `memory`，显式配置 `elasticsearch` 时启用 ES 实现。
- 已新增 `ElasticsearchVectorStore`，使用 JDK `HttpClient` 调用 ES REST API。
- 已支持自动创建 ES index mapping，向量字段使用 `dense_vector`。
- 已支持 bulk 写入 chunk embedding，并保存正文、标题、来源、页码、tags、indexVersion 等兜底字段。
- 已支持 `script_score + cosineSimilarity` 精确向量检索，并将 ES 分数转回 `0-1` 区间。
- 已支持 tags 过滤和按 documentId 删除向量文档。
- 已调整检索服务：内存 chunk 仓储缺失时，可以使用 ES 返回的持久化字段组装 search/query 上下文。
- 已新增 `docs/rag-elasticsearch-vector-store.md`，记录配置示例、当前取舍和本地检查命令。
- 已补充 `ElasticsearchVectorStoreTest`，使用本地 HTTP server 模拟 ES REST API。
- 已执行 `mvn test`，结果通过：32 个测试，0 failure，0 error。

## 正在进行

- 等待进入 Iteration 8。

## 待完成

- Iteration 8：高级检索实验。
- 基于现有 eval 先建立一组 baseline 评测结果。
- 选择一个高级检索实验作为第一轮，例如术语同义词词典或 query rewrite。
- 每个实验都要可配置关闭。
- 每个实验都要记录 eval 前后对比。

## 本轮暂不做

- 不接 Milvus/Qdrant 等独立向量平台。
- 不直接上复杂 GraphRAG。
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
- 测试统计：32 tests, 0 failures, 0 errors, 0 skipped
- 时间：2026-07-30 09:26 Asia/Shanghai
