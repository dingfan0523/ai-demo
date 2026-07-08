package com.aidemo.chat.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 结构化回复实体
 *
 * <p>要求 AI 严格按照此 JSON 结构返回内容，后端反序列化后封装到该对象</p>
 */
@Data
public class AiReply {

    /** AI 对用户问题的核心回答 */
    private String answer;

    /** 本次回答涉及的关键词列表 */
    private List<String> keywords;

    /** 置信度，范围 0-100 */
    private Integer confidence;

    /** 建议用户继续提问的后续问题 */
    private List<String> followUpQuestions;

    /** 问题分类，如 "技术"、"生活"、"咨询" 等 */
    private String category;
}
