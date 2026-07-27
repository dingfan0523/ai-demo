package com.aidemo.rag.dto;

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
public class RagIngestRequest {

    /** 来源类型，例如 markdown、txt、pdf。 */
    @NotBlank(message = "文档来源类型不能为空")
    private String sourceType = "markdown";

    /** 来源路径，可以是本地文件或目录。 */
    @NotBlank(message = "文档来源路径不能为空")
    private String sourcePath;

    private List<String> tags = new ArrayList<>();

    private boolean overwrite = false;
}
