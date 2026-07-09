package com.aidemo.knowledge.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 面向 Java、Agent 和大模型工程方向的轻量知识库工具。
 */
@Slf4j
@Component
public class EngineeringKnowledgeTools {

    @Tool(name = "java_topic_guide", value = {
            "Use when the user asks about Java backend knowledge, JVM, Spring, concurrency, or Java learning paths.",
            "Returns a compact knowledge guide with concepts, pitfalls, and practice tasks."
    })
    public String javaTopicGuide(@P("Java topic, for example JVM, Spring Boot, concurrency, collections, Redis, MySQL") String topic) {
        log.info("[Tool] java_topic_guide invoked | topic='{}'", topic);
        String normalizedTopic = normalize(topic);
        if (containsAny(normalizedTopic, "jvm", "垃圾回收", "gc", "内存")) {
            return """
                    Java 知识卡片：JVM / GC
                    核心概念：运行时数据区、对象创建过程、可达性分析、垃圾收集器、GC 日志。
                    常见误区：只背收集器名称，不会结合停顿时间、吞吐量和堆大小分析问题。
                    实战练习：用 -Xms/-Xmx、-XX:+PrintGCDetails 或 JFR 观察一次 Full GC 前后的内存变化。
                    推荐回答角度：先解释现象，再给排查命令，最后给调优边界。
                    """;
        }
        if (containsAny(normalizedTopic, "并发", "线程", "锁", "thread", "concurrent")) {
            return """
                    Java 知识卡片：并发编程
                    核心概念：线程池、锁、volatile、synchronized、CAS、CompletableFuture、虚拟线程。
                    常见误区：把线程数开大当成性能优化，忽略阻塞比例、队列长度和拒绝策略。
                    实战练习：设计一个有界线程池，并说明核心线程数、队列、拒绝策略的取舍。
                    推荐回答角度：区分 CPU 密集、IO 密集、异步编排和限流降级。
                    """;
        }
        if (containsAny(normalizedTopic, "spring", "springboot", "spring boot", "bean", "事务")) {
            return """
                    Java 知识卡片：Spring Boot
                    核心概念：自动配置、Bean 生命周期、AOP、事务传播、配置属性、Starter 机制。
                    常见误区：事务方法内部调用导致代理不生效，或把配置散落在业务代码中。
                    实战练习：写一个 @ConfigurationProperties 配置类，并加启动期校验。
                    推荐回答角度：先讲使用方式，再讲底层机制，最后补充排错路径。
                    """;
        }
        String result = """
                Java 知识卡片：通用后端工程
                核心概念：语言基础、集合、IO、异常、日志、测试、Spring Boot、数据库、缓存、消息队列。
                常见误区：只讲 API 用法，不讲工程边界、故障模式和验证方式。
                实战练习：围绕一个接口补齐参数校验、异常处理、单元测试和基础观测。
                推荐回答角度：概念 -> 代码示例 -> 常见坑 -> 面试/项目表达。
                """;
        log.info("[Tool] java_topic_guide returning | topic='{}' | resultLength={}", topic, result.length());
        return result;
    }

    @Tool(name = "agent_pattern_guide", value = {
            "Use when the user asks about AI agents, tool calling, planning, memory, RAG, or multi-agent workflow design.",
            "Returns suitable agent engineering patterns and implementation notes."
    })
    public String agentPatternGuide(@P("Agent scenario or problem, for example tool calling, memory, RAG, planner executor") String scenario) {
        log.info("[Tool] agent_pattern_guide invoked | scenario='{}'", scenario);
        String normalizedScenario = normalize(scenario);
        if (containsAny(normalizedScenario, "tool", "function", "函数", "工具调用")) {
            return """
                    Agent 模式卡片：Tool Calling
                    适用场景：需要模型查询外部状态、执行确定性计算、调用业务系统或补充知识库上下文。
                    设计要点：工具名称清晰、参数少而明确、返回值短且结构稳定、工具只做一件事。
                    风险点：模型误调用、重复调用、工具返回过长、把权限动作暴露给无鉴权用户。
                    推荐落地：先做只读工具，再增加审计、限流、权限和工具调用日志。
                    """;
        }
        if (containsAny(normalizedScenario, "rag", "检索", "知识库", "向量")) {
            return """
                    Agent 模式卡片：RAG 知识库
                    适用场景：回答依赖项目文档、规范、教程、历史案例或私有资料。
                    设计要点：文档切分、元数据、召回、重排、引用来源、无法命中时明确说明。
                    风险点：片段过碎导致上下文断裂，召回内容过多导致模型抓不住重点。
                    推荐落地：先用小规模文档和人工评测集验证，再考虑向量库和重排模型。
                    """;
        }
        if (containsAny(normalizedScenario, "memory", "记忆", "长期", "上下文")) {
            return """
                    Agent 模式卡片：Memory
                    适用场景：需要跨轮保存用户偏好、项目背景、任务状态或长期事实。
                    设计要点：区分短期上下文、长期事实、任务状态和审计日志。
                    风险点：过期记忆污染回答，敏感信息被长期保存，用户无法修正错误记忆。
                    推荐落地：记忆写入要有规则，读取要标注来源，关键事实要能更新和删除。
                    """;
        }
        String result = """
                Agent 模式卡片：通用工程设计
                常见模式：ReAct、Planner-Executor、Tool Calling、RAG、Memory、Reflection/Evaluation。
                设计要点：先定义任务边界，再决定是否需要工具、记忆、检索和多步骤规划。
                风险点：为了像 Agent 而 Agent，导致链路变长、成本变高、结果更不可控。
                推荐落地：从一个可验证的只读工具或 RAG 问答开始，逐步增加动作能力。
                """;
        log.info("[Tool] agent_pattern_guide returning | scenario='{}' | resultLength={}", scenario, result.length());
        return result;
    }

    @Tool(name = "llm_engineering_checklist", value = {
            "Use when the user asks how to build, optimize, evaluate, or troubleshoot LLM applications.",
            "Returns a practical engineering checklist for LLM application development."
    })
    public String llmEngineeringChecklist(@P("LLM engineering task, for example prompt, evaluation, cost, safety, streaming, JSON output") String task) {
        log.info("[Tool] llm_engineering_checklist invoked | task='{}'", task);
        String normalizedTask = normalize(task);
        if (containsAny(normalizedTask, "json", "结构化", "格式", "schema")) {
            return """
                    大模型工程清单：结构化输出
                    1. 明确字段、类型、必填项和取值范围。
                    2. 使用模型原生 JSON/schema 能力或服务层解析校验。
                    3. 对解析失败、字段缺失、幻觉字段设置重试或降级策略。
                    4. 用固定样例做回归测试，覆盖中文、空输入和边界值。
                    """;
        }
        if (containsAny(normalizedTask, "评测", "eval", "测试", "质量")) {
            return """
                    大模型工程清单：质量评测
                    1. 建立小而稳定的黄金问题集，覆盖高频、边界和失败场景。
                    2. 同时看正确性、引用可靠性、格式稳定性、延迟和成本。
                    3. 保存 prompt、模型、温度、工具调用和响应，方便复盘。
                    4. 先用人工验收标准，再逐步引入自动评分。
                    """;
        }
        if (containsAny(normalizedTask, "成本", "token", "延迟", "性能")) {
            return """
                    大模型工程清单：成本与性能
                    1. 统计输入 token、输出 token、工具调用次数和平均延迟。
                    2. 缩短系统提示词和工具返回，避免把整篇文档塞进上下文。
                    3. 对稳定内容做缓存，对低价值请求使用更便宜模型。
                    4. 为超时、限流和供应商异常设计降级路径。
                    """;
        }
        String result = """
                大模型工程清单：通用应用开发
                1. 定义任务边界：问答、检索、工具调用、生成、审阅还是自动执行。
                2. 设计上下文：系统提示词、用户输入、知识库片段、工具结果。
                3. 设计可靠性：超时、重试、限流、解析失败、供应商异常。
                4. 设计评测：黄金问题集、人工标准、日志追踪、版本对比。
                """;
        log.info("[Tool] llm_engineering_checklist returning | task='{}' | resultLength={}", task, result.length());
        return result;
    }

    @Tool(name = "knowledge_question_router", value = {
            "Use when the user asks a broad or ambiguous question about Java, agents, or LLM engineering.",
            "Classifies the question and suggests knowledge-base tags for retrieval or answer organization."
    })
    public String knowledgeQuestionRouter(@P("User question to classify") String question) {
        log.info("[Tool] knowledge_question_router invoked | question='{}'", question);
        String normalizedQuestion = normalize(question);
        if (containsAny(normalizedQuestion, "agent", "智能体", "tool", "function", "rag", "memory", "规划")) {
            return """
                    问题分类：Agent 工程
                    推荐标签：agent-design, tool-calling, rag, memory, workflow, evaluation
                    回答组织：场景判断 -> 模式选择 -> 实现步骤 -> 风险与验证。
                    """;
        }
        if (containsAny(normalizedQuestion, "大模型", "llm", "prompt", "提示词", "token", "结构化", "流式")) {
            return """
                    问题分类：大模型工程
                    推荐标签：prompt-engineering, structured-output, streaming, token-cost, model-evaluation
                    回答组织：目标 -> 上下文设计 -> 模型调用 -> 可靠性 -> 评测。
                    """;
        }
        if (containsAny(normalizedQuestion, "java", "spring", "jvm", "并发", "线程", "后端")) {
            return """
                    问题分类：Java 后端
                    推荐标签：java-core, spring-boot, jvm, concurrency, database, cache, testing
                    回答组织：概念解释 -> 工程实践 -> 常见坑 -> 示例代码。
                    """;
        }
        String result = """
                问题分类：综合知识库
                推荐标签：learning-path, backend-engineering, ai-application, troubleshooting
                回答组织：先澄清目标，再给路径、示例和下一步练习。
                """;
        log.info("[Tool] knowledge_question_router returning | question='{}' | resultLength={}", question, result.length());
        return result;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
