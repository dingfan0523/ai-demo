# RAG 评测使用说明

## 目标

RAG 评测用于回答一个朴素但很关键的问题：这次检索和重排有没有命中我们人工认为正确的资料。

当前实现只评测检索和重排，不调用大模型做 LLM-as-judge。这样结果更容易解释，也不会产生模型调用成本。

## 接口

```http
POST /api/rag/evaluate
```

## 请求示例

```json
{
  "topK": 5,
  "rerankTopK": 2,
  "minScore": 0.0,
  "tags": ["redis"],
  "reportPath": "docs/eval-reports/redis-baseline.md",
  "cases": [
    {
      "caseId": "redis-cache-penetration",
      "question": "Redis 缓存穿透怎么处理？",
      "expectedSourceContains": ["redis"],
      "expectedKeywords": ["Bloom Filter", "缓存穿透"]
    }
  ]
}
```

也可以把用例放到 JSON 文件里：

```json
{
  "cases": [
    {
      "caseId": "redis-cache-penetration",
      "question": "Redis 缓存穿透怎么处理？",
      "expectedSourceContains": ["05-redis-cache-patterns"],
      "expectedKeywords": ["Bloom Filter", "空值缓存"]
    }
  ]
}
```

然后请求：

```json
{
  "casesPath": "docs/eval-cases/redis.json",
  "topK": 5,
  "rerankTopK": 2,
  "reportPath": "docs/eval-reports/redis-baseline.md"
}
```

## 字段解释

- `expectedChunkIds`：期望命中的稳定 chunk id，适合固定数据集的回归测试。
- `expectedSourceContains`：期望来源中包含的文本，例如文件名、标题、章节名。
- `expectedKeywords`：期望召回内容中出现的关键词。
- `retrievalHitRate`：topK 中是否命中期望来源。
- `rerankHitRate`：rerankTopK 中是否命中期望来源。
- `keywordHitRate`：召回内容是否覆盖期望关键词。

## 当前取舍

- 暂不评测答案忠实度，因为这需要稳定的答案生成和更复杂的人工/模型评审。
- 暂不自动入库评测数据，评测前需要先通过 `/api/rag/ingest` 准备知识库。
- 暂不比较多组参数，先通过多次调用和报告文件手工对比。
