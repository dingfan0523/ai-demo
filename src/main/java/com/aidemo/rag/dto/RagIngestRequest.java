package com.aidemo.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 文档入库请求。
 *
 * <p>用于描述一次离线入库的文档来源、标签和覆盖策略。</p>
 */
@Data
@Schema(description = "RAG 文档入库请求")
public class RagIngestRequest {

    /** 来源类型，例如 markdown、txt、pdf。 */
    @NotBlank(message = "文档来源类型不能为空")
    @Schema(description = "来源类型，例如 markdown、txt、pdf", example = "markdown", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceType = "markdown";

    /** 来源路径，可以是本地文件或目录。 */
    @NotBlank(message = "文档来源路径不能为空")
    @Schema(description = "来源路径，可以是本地文件或目录；Windows 路径建议在 JSON 中使用正斜杠", example = "D:/work/my/ai-demo/docs", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourcePath;

    @Schema(description = "文档标签，用于后续检索过滤", example = "[\"redis\", \"learning\"]")
    private List<String> tags = new ArrayList<>();

    @Schema(description = "是否覆盖同来源已入库内容", example = "false")
    private boolean overwrite = false;
}
