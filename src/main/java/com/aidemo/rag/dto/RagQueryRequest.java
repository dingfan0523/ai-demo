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
 * RAG 问答请求。
 *
 * <p>会经历检索、重排、上下文组装和模型生成，最终返回答案和引用来源。</p>
 */
@Data
public class RagQueryRequest {

    /** 用户原始问题。 */
    @NotBlank(message = "问题内容不能为空")
    private String question;

    private String provider;

    /** 初筛召回数量；为空时使用配置默认值。 */
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 50, message = "topK 不能大于 50")
    private Integer topK;

    /** 重排后用于生成答案的上下文数量；为空时使用配置默认值。 */
    @Min(value = 1, message = "rerankTopK 不能小于 1")
    @Max(value = 50, message = "rerankTopK 不能大于 50")
    private Integer rerankTopK;

    /** 最小召回分数；为空时使用配置默认值。 */
    @DecimalMin(value = "0.0", message = "最小分数不能低于 0")
    @DecimalMax(value = "1.0", message = "最小分数不能高于 1")
    private Double minScore;

    private List<String> tags = new ArrayList<>();

    private boolean requireCitation = true;

    private boolean answerOnlyFromContext = true;
}
