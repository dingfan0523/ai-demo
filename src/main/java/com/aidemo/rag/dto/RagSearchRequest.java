package com.aidemo.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "RAG 检索请求")
public class RagSearchRequest {

    /** 用户检索问题或关键词。 */
    @NotBlank(message = "检索问题不能为空")
    @Schema(description = "用户检索问题或关键词", example = "Redis 缓存穿透怎么解决？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    /** 召回数量；为空时使用 `RagProperties` 默认值。 */
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 50, message = "topK 不能大于 50")
    @Schema(description = "初筛召回数量；为空时使用 RAG 默认配置", example = "8", minimum = "1", maximum = "50")
    private Integer topK;

    /** 最小召回分数；为空时使用配置默认值。 */
    @DecimalMin(value = "0.0", message = "最小分数不能低于 0")
    @DecimalMax(value = "1.0", message = "最小分数不能高于 1")
    @Schema(description = "最小召回分数；为空时使用 RAG 默认配置", example = "0.15", minimum = "0.0", maximum = "1.0")
    private Double minScore;

    @Schema(description = "标签过滤；为空时不过滤标签", example = "[\"redis\"]")
    private List<String> tags = new ArrayList<>();

    @Schema(description = "是否在命中结果中返回内容预览", example = "true")
    private boolean includeContent = true;
}
