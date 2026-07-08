package com.aidemo.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 问答请求参数
 */
@Data
public class ChatRequest {

    /** 用户输入的消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 指定使用的模型提供者（可选）
     *
     * <p>如 chatgpt、deepseek，不指定则使用默认可用模型</p>
     */
    private String provider;
}
