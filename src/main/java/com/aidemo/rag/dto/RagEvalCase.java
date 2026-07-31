package com.aidemo.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 手工评测用例。
 *
 * <p>一个用例描述一个问题以及期望命中的来源和关键词。学习阶段先用人工可解释规则，
 * 不引入 LLM-as-judge，避免评测本身变成黑盒。</p>
 */
@Data
@Schema(description = "RAG 手工评测用例")
public class RagEvalCase {

    /** 用例编号，便于在报告中定位失败样本。 */
    @Schema(description = "用例编号，便于在报告中定位失败样本", example = "redis-cache-001")
    private String caseId;

    /** 用户问题。 */
    @NotBlank(message = "评测问题不能为空")
    @Schema(description = "评测问题", example = "Redis 缓存穿透有哪些常见解决方案？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    /** 期望命中的 chunk id，适合稳定测试或回归测试。 */
    @Schema(description = "期望命中的 chunk id，适合稳定测试或回归测试", example = "[\"redis-cache-patterns-and-production-pitfalls.md#chunk-3\"]")
    private List<String> expectedChunkIds = new ArrayList<>();

    /** 期望来源包含的文本片段，例如文件名、标题、章节名。 */
    @Schema(description = "期望来源包含的文本片段，例如文件名、标题、章节名", example = "[\"cache-patterns\", \"缓存穿透\"]")
    private List<String> expectedSourceContains = new ArrayList<>();

    /** 期望召回内容中覆盖的关键词。 */
    @Schema(description = "期望召回内容中覆盖的关键词", example = "[\"布隆过滤器\", \"空值缓存\"]")
    private List<String> expectedKeywords = new ArrayList<>();

    /** 当前用例单独使用的标签过滤；为空时使用评测请求的全局 tags。 */
    @Schema(description = "当前用例单独使用的标签过滤；为空时使用评测请求的全局 tags", example = "[\"redis\"]")
    private List<String> tags = new ArrayList<>();

    /** 人工备注，例如期望答案要点或失败排查提示。 */
    @Schema(description = "人工备注，例如期望答案要点或失败排查提示", example = "重点观察是否命中缓存异常治理章节。")
    private String notes;
}
