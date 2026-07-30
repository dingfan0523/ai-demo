package com.aidemo.rag.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 评测请求。
 *
 * <p>可以直接传入 cases，也可以传入 JSON 文件路径 `casesPath`。两者同时存在时会合并执行，
 * 便于先在接口里试小样本，再沉淀成长期回归用例文件。</p>
 */
@Data
public class RagEvalRequest {

    /** 可选：评测用例 JSON 文件路径，支持数组格式或包含 cases 字段的对象格式。 */
    private String casesPath;

    /** 直接传入的评测用例。 */
    @Valid
    private List<RagEvalCase> cases = new ArrayList<>();

    /** 检索 topK；为空时使用 RAG 默认配置。 */
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 50, message = "topK 不能大于 50")
    private Integer topK;

    /** 用于统计 rerank hit 的前 N 个结果；为空时使用 RAG 默认配置。 */
    @Min(value = 1, message = "rerankTopK 不能小于 1")
    @Max(value = 50, message = "rerankTopK 不能大于 50")
    private Integer rerankTopK;

    /** 最小召回分数；为空时使用 RAG 默认配置。 */
    @DecimalMin(value = "0.0", message = "最小分数不能低于 0")
    @DecimalMax(value = "1.0", message = "最小分数不能高于 1")
    private Double minScore;

    /** 全局标签过滤；用例未配置 tags 时使用。 */
    private List<String> tags = new ArrayList<>();

    /** 可选：把本次评测报告写入指定 Markdown 文件。 */
    private String reportPath;
}
