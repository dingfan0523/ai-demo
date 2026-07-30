package com.aidemo.rag.dto;

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
public class RagEvalCase {

    /** 用例编号，便于在报告中定位失败样本。 */
    private String caseId;

    /** 用户问题。 */
    @NotBlank(message = "评测问题不能为空")
    private String question;

    /** 期望命中的 chunk id，适合稳定测试或回归测试。 */
    private List<String> expectedChunkIds = new ArrayList<>();

    /** 期望来源包含的文本片段，例如文件名、标题、章节名。 */
    private List<String> expectedSourceContains = new ArrayList<>();

    /** 期望召回内容中覆盖的关键词。 */
    private List<String> expectedKeywords = new ArrayList<>();

    /** 当前用例单独使用的标签过滤；为空时使用评测请求的全局 tags。 */
    private List<String> tags = new ArrayList<>();

    /** 人工备注，例如期望答案要点或失败排查提示。 */
    private String notes;
}
