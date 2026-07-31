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
 * RAG 问答请求。
 *
 * <p>会经历检索、重排、上下文组装和模型生成，最终返回答案和引用来源。</p>
 */
@Data
@Schema(description = "RAG 问答请求")
public class RagQueryRequest {

    /** 用户原始问题。 */
    @NotBlank(message = "问题内容不能为空")
    @Schema(description = "用户原始问题", example = "Redis 为什么需要过期删除和内存淘汰两套机制？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Schema(description = "模型提供者；为空时使用 ai.default-provider", example = "deepseek", allowableValues = {"deepseek", "chatgpt"})
    private String provider;

    /** 初筛召回数量；为空时使用配置默认值。 */
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 50, message = "topK 不能大于 50")
    @Schema(description = "初筛召回数量；为空时使用 RAG 默认配置", example = "8", minimum = "1", maximum = "50")
    private Integer topK;

    /** 重排后用于生成答案的上下文数量；为空时使用配置默认值。 */
    @Min(value = 1, message = "rerankTopK 不能小于 1")
    @Max(value = 50, message = "rerankTopK 不能大于 50")
    @Schema(description = "重排后送入模型上下文的片段数量；为空时使用 RAG 默认配置", example = "4", minimum = "1", maximum = "50")
    private Integer rerankTopK;

    /** 最小召回分数；为空时使用配置默认值。 */
    @DecimalMin(value = "0.0", message = "最小分数不能低于 0")
    @DecimalMax(value = "1.0", message = "最小分数不能高于 1")
    @Schema(description = "最小召回分数；为空时使用 RAG 默认配置", example = "0.15", minimum = "0.0", maximum = "1.0")
    private Double minScore;

    @Schema(description = "标签过滤；为空时不过滤标签", example = "[\"redis\"]")
    private List<String> tags = new ArrayList<>();

    @Schema(description = "是否要求答案带引用来源", example = "true")
    private boolean requireCitation = true;

    @Schema(description = "是否要求只基于检索上下文回答", example = "true")
    private boolean answerOnlyFromContext = true;
}
