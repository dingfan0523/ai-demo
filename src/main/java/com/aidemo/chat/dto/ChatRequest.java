package com.aidemo.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 问答请求参数
 */
@Data
@Schema(description = "AI 问答请求参数")
public class ChatRequest {

    /** 用户输入的消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户输入的消息内容", example = "请用三句话解释 Redis 缓存穿透。", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    /**
     * 指定使用的模型提供者（可选）
     *
     * <p>如 chatgpt、deepseek，不指定则使用默认可用模型</p>
     */
    @Schema(description = "模型提供者；为空时使用 ai.default-provider", example = "deepseek", allowableValues = {"deepseek", "chatgpt"})
    private String provider;
}
