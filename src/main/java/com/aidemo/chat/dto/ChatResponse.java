package com.aidemo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 问答响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** AI 结构化回复内容 */
    private AiReply reply;

    /** 实际使用的模型提供者 */
    private String provider;

    /** 实际使用的模型名称 */
    private String model;
}
