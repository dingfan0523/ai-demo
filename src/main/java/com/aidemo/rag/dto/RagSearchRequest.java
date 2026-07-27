package com.aidemo.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索请求。
 *
 * <p>只执行召回和重排，不调用大模型生成答案，适合学习阶段排查召回质量。</p>
 */
@Data
public class RagSearchRequest {

    /** 用户检索问题或关键词。 */
    @NotBlank(message = "检索问题不能为空")
    private String query;

    /** 召回数量；为空时使用 `RagProperties` 默认值。 */
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 50, message = "topK 不能大于 50")
    private Integer topK;

    /** 最小召回分数；为空时使用配置默认值。 */
    @DecimalMin(value = "0.0", message = "最小分数不能低于 0")
    @DecimalMax(value = "1.0", message = "最小分数不能高于 1")
    private Double minScore;

    private List<String> tags = new ArrayList<>();

    private boolean includeContent = true;
}
