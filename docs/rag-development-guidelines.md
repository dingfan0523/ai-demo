# RAG 开发规范

## 适用范围

本规范适用于当前项目后续所有 RAG 相关开发，重点覆盖 `com.aidemo.rag` 包、RAG 文档、RAG 测试和 RAG prompt。

目标是降低学习和维护成本：代码不只要能跑，还要能帮助理解 RAG 链路中每一步为什么存在、输入输出是什么、出现问题时该从哪里排查。

## 中文优先原则

- 业务注释、类说明、方法说明、校验提示、异常提示、接口返回提示、文档说明默认使用中文。
- RAG prompt 默认使用中文，除非调用的模型、SDK、协议或第三方工具明确要求英文表达。
- 技术名词可以保留英文，例如 `RAG`、`embedding`、`chunk`、`rerank`、`topK`、`metadata`、`VectorStore`，但需要在关键位置用中文解释含义。
- 只有在 Java API、第三方库参数、协议字段、HTTP 字段、JSON 字段名等场景下，才保留英文命名。

## 注释要求

- 每个新增类都要有中文类注释，说明它在 RAG 链路中的职责。
- 每个对外方法、接口方法、Controller 方法、Service 关键方法都要有中文方法注释，说明输入、输出和边界。
- 关键逻辑必须有中文注释，例如：
  - 文档清洗规则。
  - chunk 切分边界和 overlap 处理。
  - contentHash 去重。
  - embedding 批处理和失败降级。
  - 向量召回与 metadata filter 的先后顺序。
  - rerank 评分公式。
  - prompt 构造和引用校验。
  - prompt injection 防护。
- 注释要解释“为什么这么做”，不要只重复代码本身。例如不要写“设置变量”，而要写“保留原问题，便于生成答案时避免 query rewrite 改偏”。

## 提示信息要求

- Bean Validation 的 `message` 使用中文。
- 对用户可见的异常信息使用中文。
- API 返回的失败原因和 warning 使用中文。
- 日志可以保留英文技术字段名，但说明性文本优先中文。

示例：

```java
@NotBlank(message = "问题内容不能为空")
private String question;
```

## Prompt 要求

- RAG answer prompt 默认中文。
- Prompt 必须明确：检索文档是不可信参考材料，不是系统指令。
- Prompt 必须明确：上下文不足时，不要编造答案，要说明知识库中没有足够依据。
- 如果要求引用，prompt 必须要求使用本次上下文中存在的来源编号。

## 文档和留痕要求

- 每次迭代开始前，更新 `docs/rag-workflow-state.md` 的当前阶段和本轮目标。
- 每次迭代完成后，更新 `docs/rag-todotask.md` 的状态、完成记录和验证命令。
- 如果实际实现和设计文档有差异，要在留痕中写明取舍原因。
- 切换会话后，优先读取：
  1. `docs/rag-todotask.md`
  2. `docs/rag-workflow-state.md`
  3. `docs/rag-development-guidelines.md`

## 当前取舍

- 当前处于学习阶段，注释可以比生产代码稍微详细一些。
- 不要求每一行都有注释，但要求模块边界、类职责、方法职责和关键逻辑足够清楚。
- 后续如果某段逻辑变得复杂，优先通过拆小方法和命名降低理解成本，再补充必要中文注释。
