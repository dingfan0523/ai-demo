# RAG 模块 TodoTask

## 目标

为当前 AI Demo 构建一个学习型 RAG 模块。优先级不是一开始做成生产级知识平台，而是让完整 RAG 链路足够清晰、可观察、可调试：

离线阶段：文档解析 -> 内容清洗 -> 文档切分 -> 元数据补齐 -> embedding -> 向量存储 -> 来源追踪。

在线阶段：问题标准化 -> 问题向量化 -> 召回 -> 重排 -> 上下文组装 -> 答案生成 -> 引用返回。

每个阶段都应该足够小，便于运行、观察和对比。

## 设计原则

- RAG 放在独立包中，例如 `com.aidemo.rag`，不要把索引和检索逻辑混进 `chat`。
- 保留独立的 `/api/rag/search`，不要只做 `/api/rag/query`，这样可以单独排查召回质量。
- 每个 chunk 都必须携带元数据；来源追踪是 RAG 的核心能力，不是附加字段。
- 第一版从简单本地向量存储开始，通过 `VectorStore` 接口为后续替换留出口。
- 先做可解释重排，再考虑模型化 rerank。
- 知识库文档要当作不可信内容处理，不能让文档文本变成系统指令。
- 每次迭代都要通过响应字段、日志和小型评测用例保持可观察。
- 后续 RAG 开发遵循 `docs/rag-development-guidelines.md`：关键逻辑、类、方法使用中文注释说明；校验 message、用户可见提示和 RAG prompt 默认使用中文。

## Iteration 0 - 模块骨架与契约

状态：DONE

目标：建立 RAG 模块边界和 DTO 契约，先不一次性解决所有文档格式和向量数据库问题。

范围：

- 新增 `src/main/java/com/aidemo/rag` 包边界。
- 新增 `RagProperties`，配置 chunk 大小、overlap、topK、minScore、最大上下文长度和 debug 开关。
- 新增 ingest、search、query、chunk hit、source citation、trace steps 等 DTO。
- 新增或预留 parser、chunker、embedding、vector store、retrieval、rerank、context assembly、answer generation 等类或接口。
- 如果实际实现取舍和本文档不同，补充一段简短架构说明。

验收：

- 模块可以编译通过。
- RAG 相关类不依赖 MCP 内部实现。
- 现有 `/api/chat` 行为不受影响。

完成记录：

- 已新增 `com.aidemo.rag` 模块骨架、配置、DTO、领域模型、仓储接口、服务接口、向量存储接口和安全接口。
- 已新增 `RagPropertiesTest` 验证默认配置。
- 已新增 `RagModuleBoundaryTest` 验证 RAG 模块不依赖 MCP 内部实现。
- 已执行 `mvn test`，结果通过：14 个测试，0 failure，0 error。

暂不做：

- 真实 PDF/OCR 支持。
- 外部向量数据库。
- 模型化 rerank。

## Iteration 1 - Markdown 入库与可观察切分

状态：DONE

目标：支持导入 Markdown 文档，并能在生成答案前观察解析和切分结果。

范围：

- 支持从配置的本地知识库目录读取 Markdown 文件。
- 清洗常见噪声：重复空行、目录链接、无意义导航文本。
- 优先按 Markdown 标题和段落切分，超长 chunk 再按长度拆分。
- 相邻 chunk 增加 overlap。
- 保留元数据：`docId`、`chunkId`、`sourcePath`、`title`、`titlePath`、`sectionTitle`、`chunkIndex`、`startLine`、`endLine`、`contentHash`、`tags`、`createdAt`。
- 提供 `POST /api/rag/ingest`。
- 入库响应返回 document 数、chunk 数、跳过数和 index version。

为什么这样做：

- Markdown 最容易观察，足够支撑第一轮 RAG 入库学习。
- 按标题切分比固定长度切分更适合做引用和溯源。
- 基于 hash 跳过未变更文档，可以避免重复 chunk 污染召回结果。

对比方案：

- 固定长度切分：实现最快，但容易切断标题、示例和代码块。
- 语义切分：潜在效果更好，但更难调试，不适合第一轮动手。

验收：

- 同一未变更文件重复入库，不会生成重复 chunk。
- 返回的 chunk 包含来源路径和行号范围。
- 单元测试覆盖标题切分、超长 chunk 拆分、overlap 和 hash 跳过。

完成记录：

- 已新增 `RagController`，提供 `POST /api/rag/ingest`。
- 已新增 Markdown 入库实现：`MarkdownDocumentParser`、`MarkdownDocumentChunker`、`DefaultRagIngestService`。
- 已新增基础内容清洗器 `BasicRagContentSanitizer`。
- 已新增内存文档/切片仓储：`InMemoryKnowledgeDocumentRepository`、`InMemoryKnowledgeChunkRepository`。
- 已实现基于 `contentHash` 的重复入库跳过。
- 已实现标题结构优先、长度兜底和 overlap 的 Markdown chunk 切分。
- 已补充测试：`BasicRagContentSanitizerTest`、`MarkdownDocumentChunkerTest`、`DefaultRagIngestServiceTest`。
- 已执行 `mvn test`，结果通过：17 个测试，0 failure，0 error。

## Iteration 2 - 基础 Embedding 与本地向量存储

状态：DONE

目标：跑通最小可用的向量检索闭环。

范围：

- 新增 `EmbeddingService` 抽象。
- 新增一个简单 embedding provider 适配器，配置和聊天模型分开。
- 新增 `VectorStore` 抽象。
- 新增本地向量存储实现。
- 向量记录保存 chunk id、embedding model、vector dimension、index version 等信息。
- 提供 `POST /api/rag/search`，用于只检索、不生成答案。

为什么这样做：

- embedding 模型变化时，不应该重写文档和 chunk 数据。
- 本地向量存储可以先避免数据库部署成本，让第一轮链路能跑起来。
- 检索接口独立出来后，可以判断问题到底出在召回还是答案生成。

对比方案：

- 第一版直接上 pgvector：更贴近真实后端工程，但会在理解 RAG 主链路前增加数据库配置成本。
- 第一版直接上 Milvus/Qdrant：向量能力更强，但对学习阶段来说基础设施过重。

验收：

- 查询能返回 topK chunk hit，包含 vector score 和来源元数据。
- 响应可配置是否包含内容预览，便于 debug。
- topK 和 minScore 受到配置上限保护。

完成记录：

- 已新增学习阶段本地 embedding 实现：`LocalHashEmbeddingService`。
- 已新增内存向量存储：`InMemoryVectorStore`。
- 已在 Markdown 入库后为 chunk 生成并保存向量。
- 已新增 `POST /api/rag/search`，用于只检索、不生成答案。
- search 响应返回命中 chunk、vector score、source、contentPreview 和 trace steps。
- 已补充测试：`LocalHashEmbeddingServiceTest`、`InMemoryVectorStoreTest`、`DefaultRetrievalServiceTest`。
- 已执行 `mvn test`，结果通过：20 个测试，0 failure，0 error。

## Iteration 3 - 混合召回与可解释 Rerank

状态：DONE

目标：提升对类名、配置项、API 路径、错误码、中英文混合术语等技术问题的检索效果。

范围：

- 新增关键词评分或简化 BM25 评分。
- 增加适合中文的分词或 n-gram 匹配。
- 综合 vector score、keyword score、title match、tag match、recency boost。
- 返回原始分数和最终 rerank 分数。
- 增加 trace steps，说明某个 chunk 为什么排名靠前。

为什么这样做：

- 纯向量检索可能漏掉精确技术术语。
- 纯关键词检索可能漏掉语义相近内容。
- 可解释 rerank 比一开始使用黑盒 rerank 模型更适合学习和调试。

初始评分公式：

```text
finalScore = vectorScore * 0.60
           + keywordScore * 0.25
           + titleMatchBoost * 0.10
           + metadataBoost * 0.05
```

对比方案：

- 只做向量检索：简单，但对精确术语较弱。
- 直接接专用 rerank 模型：效果可能更好，但会增加成本、延迟和调试难度。

验收：

- search 响应展示 `vectorScore`、`keywordScore`、`rerankScore`。
- 包含精确 Java/Spring/RAG 术语的问题，能把标题或关键词命中的 chunk 排到更前。
- 测试覆盖中文术语、英文标识符和中英文混合查询。

完成记录：

- 已新增 `RagTextTokenizer`，支持英文标识符、数字、中文单字和中文 bigram token，用于学习阶段的中英文混合检索。
- 已新增 `KeywordScoringService` 和 `KeywordScore`，返回关键词得分和命中的 token，便于观察问题命中了哪些证据。
- 已新增 `ExplainableRerankService`，按 vector score、keyword score、标题命中和元数据命中合成最终 `rerankScore`。
- 已将 `DefaultRetrievalService` 调整为混合召回：先扩大向量候选，再合并关键词候选，最后执行可解释 rerank。
- search 响应已返回 `vectorScore`、`keywordScore`、`rerankScore`、`matchedTokens` 和 `trace steps`。
- 已补充测试：`RagTextTokenizerTest`、`KeywordScoringServiceTest`、`ExplainableRerankServiceTest`，并更新 `DefaultRetrievalServiceTest`。
- 已执行 `mvn test`，结果通过：23 个测试，0 failure，0 error。

## Iteration 4 - 带引用的 RAG 答案生成

状态：TODO

目标：基于召回上下文生成答案，并返回可信来源。

范围：

- 新增 `POST /api/rag/query`。
- 构造 RAG prompt，明确检索文档只是“不可信参考材料”。
- 默认要求模型只基于上下文回答。
- 返回 `answer`、`sources`、`contexts`、`provider`、`model`、`traceId`、`confidence`。
- 增加引用校验，模型返回的 source id 必须存在于本次 context 列表。
- 在不把检索逻辑塞进 `ChatService` 的前提下，尽量复用现有聊天 provider 选择模式。

为什么这样做：

- 引用来源是普通聊天和可信 RAG 的关键差异。
- 学习阶段返回 contexts，可以直观看到模型到底看到了哪些证据。
- 引用校验可以发现模型编造来源编号的问题。

对比方案：

- 不返回引用：实现更简单，但失去可信度和可调试性。
- 不返回 contexts：更接近生产环境安全策略，但不利于学习排查。

验收：

- 检索为空或置信度过低时，返回“知识库中没有足够依据”一类的明确回答。
- 每个返回引用都能映射到本次召回的 chunk。
- 现有 `/api/chat` 测试继续通过。

## Iteration 5 - PDF 与混合文档解析

状态：TODO

目标：在核心链路跑通后，把文档入库从 Markdown 扩展到 PDF 等混合文档。

范围：

- 增加 PDF 文本抽取。
- 在元数据中保留页码。
- 识别图片占比高或扫描型 PDF，并返回 warning。
- 可选 OCR 解析器作为后续 adapter，不作为默认路径。
- 增加解析诊断信息：抽取文本长度、页数、warning 列表、忽略元素。

为什么这样做：

- PDF 解析质量会直接影响 chunk 质量。
- OCR 和版面解析会引入噪声，应该显式暴露诊断结果，而不是静默相信。

对比方案：

- Apache PDFBox：Java 原生，适合文本型 PDF；对复杂版面和 OCR 较弱。
- Apache Tika：格式支持更广，但依赖更重。
- OCR/Tesseract：适合扫描件，但噪声多、环境要求高。
- 类 Unstructured 的版面解析：混合文档效果强，但组件更多。

验收：

- 文本型 PDF 可以入库，并保留页码元数据。
- 扫描型或图片型 PDF 会被识别并给出 warning，而不是生成误导性空 chunk。
- ingest 响应返回 parser warnings。

## Iteration 6 - RAG 评测体系

状态：TODO

目标：建立一个小型回归评测工具，用来判断 RAG 调参后是否真的变好。

范围：

- 增加 10-20 个手工评测用例。
- 每个用例保存 question、expected source、expected keywords、可选 expected answer。
- 记录 retrieval hit rate、rerank hit rate、citation accuracy、answer faithfulness notes、latency、可选 token cost。
- 支持对比不同 chunk size、topK、rerank 权重和 embedding model。

为什么这样做：

- RAG 很容易“感觉变好了”，但实际召回变差。
- 小型黄金集足够发现明显回归。

对比方案：

- LLM-as-judge：后续有用，但第一阶段过于黑盒。
- 纯人工测试：快，但容易遗漏边界用例。

验收：

- eval 结果能展示哪些用例在 topK 和 rerankTopK 中命中了期望来源。
- 修改 chunk 参数后，可以和上一轮结果对比。

## Iteration 7 - 持久化向量存储

状态：TODO

目标：在保持模块契约不变的前提下，把本地向量存储替换为更真实的后端存储。

推荐路径：

1. PostgreSQL + pgvector
2. 只有当规模或向量平台能力本身成为学习重点时，再考虑 Milvus/Qdrant

范围：

- 增加持久化 document/chunk/embedding 存储。
- 增加 index version 和 rebuild 支持。
- 查询时支持 metadata filter。
- 增加迁移说明和本地启动文档。

为什么这样做：

- 持久化后，反复实验才接近真实场景。
- pgvector 能把 metadata 和 vector 放在普通后端数据库附近，比单独运维向量平台更容易学习。

验收：

- 应用重启后索引向量不丢失。
- 更换 embedding model 后，可以重建 embedding，而不重写源文档。
- 搜索结果仍符合本地向量存储时期的接口契约。

## Iteration 8 - 高级检索实验

状态：TODO

目标：在 baseline 可评测后，再尝试更成熟的 RAG 优化能力。

候选实验：

- LLM query rewrite。
- Multi-query expansion。
- Java/Spring/AI 术语同义词词典。
- 模型化 rerank。
- Parent-child retrieval：小 chunk 用于检索，大 parent section 用于回答上下文。
- Contextual compression。
- 多模态文档解析。

为什么这样做：

- 这些技术只有在 baseline 可测量时才有意义。

验收：

- 每个实验都有前后 eval 对比结果。
- 每个实验都可以通过配置关闭。

## 待确认问题

- 第一版 embedding provider 用哪个？
- 第一版存储用纯内存、JSON 持久化，还是直接 pgvector？
- 默认知识库目录放在哪里？
- `/api/rag/query` 是直接复用现有 `ChatService`，还是通过专门的 RAG answer adapter 调用更底层 provider？
- debug 响应默认返回完整 chunk 内容，还是只返回内容预览？

## 暂不做

- 生产级认证和多用户权限系统。
- 完整 OCR 管线。
- GraphRAG。
- 分布式索引任务。
- 外部网站自动爬取。
- 替换现有 `EngineeringKnowledgeTools`。
