package com.aidemo.rag.service;

import com.aidemo.chat.dto.AiReply;
import com.aidemo.chat.provider.AiProviderClient;
import com.aidemo.model.config.ModelProperties;
import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagQueryRequest;
import com.aidemo.rag.dto.RagQueryResponse;
import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;
import com.aidemo.rag.dto.RagSource;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRagAnswerServiceTest {

    @Test
    void answerGeneratesWithContextAndRemovesInvalidCitation() {
        RagChunkHit hit = hit("chunk-redis", "Redis 缓存", "缓存穿透可以使用布隆过滤器和空值缓存缓解。", 0.91d);
        DefaultRagAnswerService service = service(
                retrievalWithHits(List.of(hit)),
                provider("deepseek", "deepseek-test", "可以使用布隆过滤器拦截不存在的 key [source:chunk-redis]。错误来源会被剔除 [source:missing]。", null)
        );
        service.afterPropertiesSet();

        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("Redis 缓存穿透怎么解决？");

        RagQueryResponse response = service.answer(request);

        assertThat(response.getAnswer()).contains("[source:chunk-redis]");
        assertThat(response.getAnswer()).doesNotContain("[source:missing]");
        assertThat(response.getSources()).extracting("chunkId").containsExactly("chunk-redis");
        assertThat(response.getContexts()).containsExactly(hit);
        assertThat(response.getProvider()).isEqualTo("deepseek");
        assertThat(response.getModel()).isEqualTo("deepseek-test");
        assertThat(response.getConfidence()).isEqualTo(91);
        assertThat(response.getSteps()).extracting("name")
                .contains("context_assembly", "answer_generation", "citation_validation");
        assertThat(response.getSteps()).filteredOn(step -> "citation_validation".equals(step.getName()))
                .extracting("status")
                .containsExactly("warning");
    }

    @Test
    void answerSkipsModelWhenRetrievalHasNoContext() {
        AtomicInteger modelCallCount = new AtomicInteger();
        DefaultRagAnswerService service = service(
                retrievalWithHits(List.of()),
                provider("deepseek", "deepseek-test", "不应该调用模型", modelCallCount)
        );
        service.afterPropertiesSet();

        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("知识库里没有的问题");

        RagQueryResponse response = service.answer(request);

        assertThat(response.getAnswer()).contains("知识库中没有足够依据");
        assertThat(response.getConfidence()).isZero();
        assertThat(response.getSources()).isEmpty();
        assertThat(modelCallCount).hasValue(0);
        assertThat(response.getSteps()).extracting("name").contains("low_confidence_guard");
    }

    private DefaultRagAnswerService service(RetrievalService retrievalService, AiProviderClient provider) {
        RagProperties ragProperties = new RagProperties();
        ragProperties.setRerankTopK(2);
        ragProperties.setMaxContextChars(4000);
        ModelProperties modelProperties = new ModelProperties();
        modelProperties.setDefaultProvider("deepseek");
        return new DefaultRagAnswerService(
                retrievalService,
                new DefaultContextAssembler(),
                ragProperties,
                modelProperties,
                List.of(provider)
        );
    }

    private RetrievalService retrievalWithHits(List<RagChunkHit> hits) {
        return new RetrievalService() {
            @Override
            public RagSearchResponse search(RagSearchRequest request) {
                RagSearchResponse response = new RagSearchResponse();
                response.setTraceId("search-test");
                response.setOriginalQuery(request.getQuery());
                response.setRewrittenQuery(request.getQuery());
                response.setHits(hits);
                return response;
            }
        };
    }

    private AiProviderClient provider(String name, String modelName, String answer, AtomicInteger callCount) {
        return new AiProviderClient() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String modelName() {
                return modelName;
            }

            @Override
            public String chat(String message) {
                if (callCount != null) {
                    callCount.incrementAndGet();
                }
                assertThat(message).contains("参考材料");
                assertThat(message).contains("[source:chunk-redis]");
                return answer;
            }

            @Override
            public Flux<String> chatStream(String message) {
                return Flux.just(answer);
            }

            @Override
            public AiReply chatStructured(String message) {
                AiReply reply = new AiReply();
                reply.setAnswer(answer);
                return reply;
            }
        };
    }

    private RagChunkHit hit(String chunkId, String title, String content, double rerankScore) {
        RagSource source = new RagSource();
        source.setChunkId(chunkId);
        source.setDocumentId("doc-redis");
        source.setTitle(title);
        source.setSourceUri("redis.md");
        source.setSectionTitle("缓存穿透");
        source.setStartLine(10);
        source.setEndLine(18);
        source.setRerankScore(rerankScore);

        RagChunkHit hit = new RagChunkHit();
        hit.setChunkId(chunkId);
        hit.setDocumentId("doc-redis");
        hit.setTitle(title);
        hit.setContent(content);
        hit.setContentPreview(content);
        hit.setVectorScore(0.8d);
        hit.setKeywordScore(0.7d);
        hit.setRerankScore(rerankScore);
        hit.setSource(source);
        return hit;
    }
}
