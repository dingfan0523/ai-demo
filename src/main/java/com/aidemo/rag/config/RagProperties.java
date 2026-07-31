package com.aidemo.rag.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * RAG 模块配置。
 *
 * <p>这些配置控制学习型 RAG 的默认行为，例如知识库目录、chunk 大小、召回数量、
 * 最小分数和 debug 输出。后续接入真实向量库或 embedding provider 时，也应优先
 * 通过这里集中配置，避免把参数散落到业务逻辑中。</p>
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = false;

    /** 本地知识库目录，第一阶段默认面向 Markdown 文档。 */
    @NotBlank(message = "RAG 知识库目录不能为空")
    private String knowledgePath = "src/main/resources/knowledge";

    /** 单个 chunk 的目标大小，后续切分器会在语义边界和长度之间做取舍。 */
    @Min(value = 100, message = "RAG chunk 大小不能小于 100")
    @Max(value = 4000, message = "RAG chunk 大小不能大于 4000")
    private int chunkSize = 800;

    /** 相邻 chunk 的重叠长度，用来缓解上下文被切断的问题。 */
    @Min(value = 0, message = "RAG chunk overlap 不能为负数")
    @Max(value = 1000, message = "RAG chunk overlap 不能大于 1000")
    private int chunkOverlap = 120;

    /** 初筛召回数量上限，避免一次检索返回过多上下文。 */
    @Min(value = 1, message = "RAG topK 不能小于 1")
    @Max(value = 50, message = "RAG topK 不能大于 50")
    private int topK = 8;

    /** 重排后进入答案生成阶段的候选数量。 */
    @Min(value = 1, message = "RAG rerankTopK 不能小于 1")
    @Max(value = 50, message = "RAG rerankTopK 不能大于 50")
    private int rerankTopK = 4;

    /** 最小召回分数，低于该分数的片段默认不进入上下文。 */
    @DecimalMin(value = "0.0", message = "RAG 最小分数不能低于 0")
    @DecimalMax(value = "1.0", message = "RAG 最小分数不能高于 1")
    private double minScore = 0.3;

    /** 组装给模型的最大上下文长度，防止召回内容过多挤占 prompt。 */
    @Min(value = 1000, message = "RAG 最大上下文长度不能小于 1000")
    @Max(value = 100000, message = "RAG 最大上下文长度不能大于 100000")
    private int maxContextChars = 8000;

    /** 当前索引使用的 embedding 模型名称，后续用于索引版本和重建判断。 */
    @NotBlank(message = "RAG embedding 模型不能为空")
    private String embeddingModel = "local-learning-embedding";

    @Valid
    @NotNull(message = "RAG 向量存储配置不能为空")
    private VectorStore vectorStore = new VectorStore();

    @Valid
    @NotNull(message = "RAG Elasticsearch 配置不能为空")
    private Elasticsearch elasticsearch = new Elasticsearch();

    @Valid
    @NotNull(message = "RAG debug 配置不能为空")
    private Debug debug = new Debug();

    /**
     * 向量存储选择。
     *
     * <p>默认继续使用 memory，只有显式配置为 elasticsearch 时才启用 ES 实现，
     * 避免学习阶段因为本地 ES 未启动而影响现有链路。</p>
     */
    @Data
    public static class VectorStore {

        @NotBlank(message = "RAG 向量存储类型不能为空")
        private String type = "elasticsearch";
    }

    /**
     * Elasticsearch 向量存储配置。
     *
     * <p>当前按 ES 7.17 兼容方式实现：dense_vector + script_score 精确向量检索。
     * 后续升级到 ES 8/9 原生 knn 时，可以继续在这里扩展 searchMode。</p>
     */
    @Data
    public static class Elasticsearch {

        @NotBlank(message = "RAG Elasticsearch 地址不能为空")
        private String baseUrl = "http://localhost:9200";

        @NotBlank(message = "RAG Elasticsearch 索引名不能为空")
        private String indexName = "ai_demo_rag_chunks";

        @Min(value = 1, message = "RAG Elasticsearch 请求超时不能小于 1 秒")
        @Max(value = 300, message = "RAG Elasticsearch 请求超时不能大于 300 秒")
        private int requestTimeoutSeconds = 30;

        /** 当前默认只实现 script_score，后续可以扩展 knn。 */
        @NotBlank(message = "RAG Elasticsearch 搜索模式不能为空")
        private String searchMode = "script_score";
    }

    /**
     * Debug 输出配置。
     *
     * <p>学习阶段默认打开更多可观察信息，便于理解召回、重排和上下文组装结果。</p>
     */
    @Data
    public static class Debug {

        private boolean includeContext = true;

        private boolean includeTraceSteps = true;

        private boolean includeScores = true;
    }
}
