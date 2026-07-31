# RAG Elasticsearch 向量存储说明

## 目标

当前实现用于学习阶段的持久化向量存储，兼容本机 `Elasticsearch 7.17.9`。

实现方式：

- 向量字段：`dense_vector`
- 检索方式：`script_score + cosineSimilarity`
- 默认索引：`ai_demo_rag_chunks`
- 默认地址：`http://localhost:9200`

这不是大规模生产检索方案。ES 7.17 的 `script_score` 会对候选文档做精确打分，更适合小数据量、可解释、可调试的 RAG 学习场景。

## 配置示例

不要把本地密钥写入 `application.yml`。如果要启用 ES 向量存储，可以用本地环境变量或启动参数：

```bash
java -jar target/ai-demo-1.0.0.jar \
  --rag.vector-store.type=elasticsearch \
  --rag.elasticsearch.base-url=http://localhost:9200 \
  --rag.elasticsearch.index-name=ai_demo_rag_chunks
```

开发时也可以在 IDE 的 VM options / Program arguments 中加入：

```text
--rag.vector-store.type=elasticsearch
--rag.elasticsearch.base-url=http://localhost:9200
--rag.elasticsearch.index-name=ai_demo_rag_chunks
```

## Mapping 要点

索引会在第一次写入向量时自动创建。核心字段包括：

- `documentId`
- `chunkId`
- `content`
- `contentPreview`
- `title`
- `sourceUri`
- `sectionTitle`
- `startLine/endLine`
- `pageStart/pageEnd`
- `tags`
- `embeddingModel`
- `indexVersion`
- `vector`

`vector` 的维度来自当前 embedding 结果。当前本地 hash embedding 是 128 维。

## 检索逻辑

ES 查询使用：

```text
cosineSimilarity(params.queryVector, 'vector') + 1.0
```

ES 返回分数是 `cosine + 1.0`，代码会再转回 `0-1` 区间，保持和内存版 `VectorStore` 的分数语义一致。

## 当前取舍

- 保留 `InMemoryVectorStore`，默认仍使用内存实现。
- 只有 `rag.vector-store.type=elasticsearch` 时才启用 ES 实现。
- ES 文档中保存了 chunk 正文和来源字段，因此应用重启后即使内存 chunk 仓储为空，也能用 ES 结果兜底返回内容。
- 关键词候选补充仍依赖当前内存 chunk 仓储；应用重启后如果没有重新入库，搜索会退化为 ES 向量候选 + 可解释 rerank。
- 暂不使用 ES 8/9 native kNN，后续可以在 `rag.elasticsearch.search-mode` 中扩展。

## 本地检查

确认 ES 是否启动：

```bash
curl http://localhost:9200
```

查看索引 mapping：

```bash
curl http://localhost:9200/ai_demo_rag_chunks/_mapping
```

删除学习阶段索引：

```bash
curl -X DELETE http://localhost:9200/ai_demo_rag_chunks
```
