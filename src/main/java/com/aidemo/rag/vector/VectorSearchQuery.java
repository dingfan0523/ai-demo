package com.aidemo.rag.vector;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量检索条件。
 *
 * <p>封装查询向量、topK、最小分数和标签过滤条件。</p>
 */
@Data
public class VectorSearchQuery {

    private List<Double> vector = new ArrayList<>();

    private int topK;

    private double minScore;

    private List<String> tags = new ArrayList<>();
}
