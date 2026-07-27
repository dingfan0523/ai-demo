package com.aidemo.rag.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * embedding 向量值。
 *
 * <p>封装模型名称、维度和向量数组，避免服务层直接传递散乱的 List。</p>
 */
@Data
public class EmbeddingVector {

    private String model;

    private Integer dimension;

    private List<Double> values = new ArrayList<>();
}
