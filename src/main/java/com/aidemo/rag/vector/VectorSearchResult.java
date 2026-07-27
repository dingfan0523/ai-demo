package com.aidemo.rag.vector;

import lombok.Data;

/**
 * 向量检索结果。
 *
 * <p>只返回 chunk ID、分数和索引版本，具体 chunk 内容由上层仓储再查询。</p>
 */
@Data
public class VectorSearchResult {

    private String chunkId;

    private double score;

    private String indexVersion;
}
