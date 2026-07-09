package com.aidemo.knowledge.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EngineeringKnowledgeToolsTest {

    private final EngineeringKnowledgeTools tools = new EngineeringKnowledgeTools();

    @Test
    void javaTopicGuideReturnsConcurrencyCard() {
        String guide = tools.javaTopicGuide("Java 并发线程池怎么学");

        assertThat(guide)
                .contains("并发编程")
                .contains("线程池")
                .contains("拒绝策略");
    }

    @Test
    void agentPatternGuideReturnsRagCard() {
        String guide = tools.agentPatternGuide("知识库 RAG 检索怎么设计");

        assertThat(guide)
                .contains("RAG 知识库")
                .contains("召回")
                .contains("重排");
    }

    @Test
    void llmEngineeringChecklistReturnsStructuredOutputChecklist() {
        String checklist = tools.llmEngineeringChecklist("结构化 JSON 输出");

        assertThat(checklist)
                .contains("结构化输出")
                .contains("字段")
                .contains("解析失败");
    }

    @Test
    void knowledgeQuestionRouterClassifiesAgentQuestion() {
        String route = tools.knowledgeQuestionRouter("agent tool calling 和 memory 怎么配合");

        assertThat(route)
                .contains("Agent 工程")
                .contains("tool-calling")
                .contains("memory");
    }
}
