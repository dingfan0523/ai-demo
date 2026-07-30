package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagEvalCase;
import com.aidemo.rag.dto.RagEvalCaseResult;
import com.aidemo.rag.dto.RagEvalRequest;
import com.aidemo.rag.dto.RagEvalResponse;
import com.aidemo.rag.dto.RagSearchRequest;
import com.aidemo.rag.dto.RagSearchResponse;
import com.aidemo.rag.dto.RagSource;
import com.aidemo.rag.dto.RagTraceStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 默认 RAG 评测服务。
 *
 * <p>当前只评估检索和重排：每个问题调用现有 {@link RetrievalService}，
 * 再用手工期望来源和关键词判断是否命中。这样评测结果可解释、可复现，也不会产生模型调用成本。</p>
 */
@Service
@RequiredArgsConstructor
public class DefaultRagEvalService implements RagEvalService {

    private final RetrievalService retrievalService;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    @Override
    public RagEvalResponse evaluate(RagEvalRequest request) {
        long startedAt = System.currentTimeMillis();
        List<RagEvalCase> cases = loadCases(request);
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("评测用例不能为空，请传入 cases 或 casesPath");
        }

        RagEvalResponse response = new RagEvalResponse();
        response.setTraceId("rag-eval-" + UUID.randomUUID());
        response.getSteps().add(step("load_eval_cases", "success", "加载评测用例 " + cases.size() + " 条", startedAt));

        for (RagEvalCase evalCase : cases) {
            response.getResults().add(evaluateCase(request, evalCase));
        }

        fillSummary(response, startedAt);
        response.getSteps().add(step("summarize_eval", "success",
                "评测完成，retrievalHitRate=" + response.getRetrievalHitRate()
                        + ", rerankHitRate=" + response.getRerankHitRate(), startedAt));
        response.setReportMarkdown(buildReport(response));
        writeReportIfNeeded(request, response, startedAt);
        return response;
    }

    /**
     * 加载评测用例。
     *
     * <p>文件格式既支持数组，也支持 `{ "cases": [...] }` 对象，方便手写和接口请求复用同一份样例。</p>
     */
    private List<RagEvalCase> loadCases(RagEvalRequest request) {
        List<RagEvalCase> cases = new ArrayList<>();
        if (request.getCases() != null) {
            cases.addAll(request.getCases());
        }
        if (request.getCasesPath() != null && !request.getCasesPath().isBlank()) {
            cases.addAll(readCasesFromFile(request.getCasesPath()));
        }
        return cases;
    }

    private List<RagEvalCase> readCasesFromFile(String casesPath) {
        Path path = Path.of(casesPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("评测用例文件不存在: " + path);
        }
        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            JsonNode casesNode = root.isArray() ? root : root.get("cases");
            if (casesNode == null || !casesNode.isArray()) {
                throw new IllegalArgumentException("评测用例文件格式错误，必须是数组或包含 cases 数组: " + path);
            }
            List<RagEvalCase> cases = new ArrayList<>();
            for (JsonNode node : casesNode) {
                cases.add(objectMapper.treeToValue(node, RagEvalCase.class));
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("读取评测用例文件失败: " + path, e);
        }
    }

    private RagEvalCaseResult evaluateCase(RagEvalRequest request, RagEvalCase evalCase) {
        long startedAt = System.currentTimeMillis();
        RagSearchResponse searchResponse = retrievalService.search(toSearchRequest(request, evalCase));
        int rerankTopK = request.getRerankTopK() == null ? ragProperties.getRerankTopK() : request.getRerankTopK();
        List<RagChunkHit> hits = searchResponse.getHits();
        List<RagChunkHit> rerankHits = hits.stream().limit(rerankTopK).toList();

        RagEvalCaseResult result = new RagEvalCaseResult();
        result.setCaseId(resolveCaseId(evalCase));
        result.setQuestion(evalCase.getQuestion());
        result.setExpectedChunkIds(evalCase.getExpectedChunkIds());
        result.setExpectedSourceContains(evalCase.getExpectedSourceContains());
        result.setExpectedKeywords(evalCase.getExpectedKeywords());
        result.setNotes(evalCase.getNotes());
        result.setTopChunkIds(hits.stream().map(RagChunkHit::getChunkId).toList());
        result.setTopSources(hits.stream().map(RagChunkHit::getSource).toList());
        result.setMatchedSources(matchedSources(hits, evalCase));
        result.setMatchedKeywords(matchedKeywords(hits, evalCase));
        result.setRetrievalHit(hasExpectedSourceHit(hits, evalCase));
        result.setRerankHit(hasExpectedSourceHit(rerankHits, evalCase));
        result.setKeywordHit(keywordHit(result, evalCase));
        result.setLatencyMs(System.currentTimeMillis() - startedAt);
        result.setFailureReason(failureReason(result, evalCase));
        return result;
    }

    private RagSearchRequest toSearchRequest(RagEvalRequest request, RagEvalCase evalCase) {
        RagSearchRequest searchRequest = new RagSearchRequest();
        searchRequest.setQuery(evalCase.getQuestion());
        searchRequest.setTopK(request.getTopK() == null ? ragProperties.getTopK() : request.getTopK());
        searchRequest.setMinScore(request.getMinScore() == null ? ragProperties.getMinScore() : request.getMinScore());
        searchRequest.setTags(evalCase.getTags() == null || evalCase.getTags().isEmpty() ? request.getTags() : evalCase.getTags());
        searchRequest.setIncludeContent(true);
        return searchRequest;
    }

    private boolean hasExpectedSourceHit(List<RagChunkHit> hits, RagEvalCase evalCase) {
        if (evalCase.getExpectedChunkIds().isEmpty() && evalCase.getExpectedSourceContains().isEmpty()) {
            return !hits.isEmpty();
        }
        return hits.stream().anyMatch(hit -> matchesExpectedSource(hit, evalCase));
    }

    private boolean matchesExpectedSource(RagChunkHit hit, RagEvalCase evalCase) {
        if (evalCase.getExpectedChunkIds().contains(hit.getChunkId())) {
            return true;
        }
        String haystack = sourceText(hit).toLowerCase(Locale.ROOT);
        return evalCase.getExpectedSourceContains()
                .stream()
                .filter(expected -> expected != null && !expected.isBlank())
                .map(expected -> expected.toLowerCase(Locale.ROOT).trim())
                .anyMatch(haystack::contains);
    }

    private List<String> matchedSources(List<RagChunkHit> hits, RagEvalCase evalCase) {
        Set<String> matched = new LinkedHashSet<>();
        for (RagChunkHit hit : hits) {
            if (matchesExpectedSource(hit, evalCase)) {
                matched.add(hit.getChunkId());
            }
        }
        return new ArrayList<>(matched);
    }

    private List<String> matchedKeywords(List<RagChunkHit> hits, RagEvalCase evalCase) {
        Set<String> matched = new LinkedHashSet<>();
        String haystack = hits.stream()
                .map(this::contentText)
                .reduce("", (left, right) -> left + "\n" + right)
                .toLowerCase(Locale.ROOT);
        for (String keyword : evalCase.getExpectedKeywords()) {
            if (keyword != null && !keyword.isBlank() && haystack.contains(keyword.toLowerCase(Locale.ROOT).trim())) {
                matched.add(keyword);
            }
        }
        return new ArrayList<>(matched);
    }

    private boolean keywordHit(RagEvalCaseResult result, RagEvalCase evalCase) {
        return evalCase.getExpectedKeywords().isEmpty()
                || result.getMatchedKeywords().containsAll(evalCase.getExpectedKeywords());
    }

    private String failureReason(RagEvalCaseResult result, RagEvalCase evalCase) {
        List<String> reasons = new ArrayList<>();
        if (!result.isRetrievalHit()) {
            reasons.add("topK 未命中期望来源");
        }
        if (!result.isRerankHit()) {
            reasons.add("rerankTopK 未命中期望来源");
        }
        if (!result.isKeywordHit()) {
            List<String> missing = evalCase.getExpectedKeywords()
                    .stream()
                    .filter(keyword -> !result.getMatchedKeywords().contains(keyword))
                    .toList();
            reasons.add("缺少期望关键词: " + missing);
        }
        return reasons.isEmpty() ? "" : String.join("；", reasons);
    }

    private void fillSummary(RagEvalResponse response, long startedAt) {
        int total = response.getResults().size();
        response.setTotalCases(total);
        response.setRetrievalHitCount((int) response.getResults().stream().filter(RagEvalCaseResult::isRetrievalHit).count());
        response.setRerankHitCount((int) response.getResults().stream().filter(RagEvalCaseResult::isRerankHit).count());
        response.setKeywordHitCount((int) response.getResults().stream().filter(RagEvalCaseResult::isKeywordHit).count());
        response.setRetrievalHitRate(rate(response.getRetrievalHitCount(), total));
        response.setRerankHitRate(rate(response.getRerankHitCount(), total));
        response.setKeywordHitRate(rate(response.getKeywordHitCount(), total));
        response.setTotalLatencyMs(System.currentTimeMillis() - startedAt);
        response.setAverageLatencyMs(total == 0 ? 0.0d : response.getResults()
                .stream()
                .mapToLong(RagEvalCaseResult::getLatencyMs)
                .average()
                .orElse(0.0d));
    }

    private String buildReport(RagEvalResponse response) {
        StringBuilder report = new StringBuilder();
        report.append("# RAG 评测报告\n\n");
        report.append("- 总用例数：").append(response.getTotalCases()).append("\n");
        report.append("- retrieval hit rate：").append(response.getRetrievalHitRate()).append("\n");
        report.append("- rerank hit rate：").append(response.getRerankHitRate()).append("\n");
        report.append("- keyword hit rate：").append(response.getKeywordHitRate()).append("\n");
        report.append("- 平均耗时(ms)：").append(String.format(Locale.ROOT, "%.2f", response.getAverageLatencyMs())).append("\n\n");
        report.append("| 用例 | Retrieval | Rerank | Keyword | 耗时(ms) | 失败原因 |\n");
        report.append("| --- | --- | --- | --- | ---: | --- |\n");
        for (RagEvalCaseResult result : response.getResults()) {
            report.append("| ")
                    .append(escapeTable(result.getCaseId()))
                    .append(" | ")
                    .append(result.isRetrievalHit() ? "命中" : "未命中")
                    .append(" | ")
                    .append(result.isRerankHit() ? "命中" : "未命中")
                    .append(" | ")
                    .append(result.isKeywordHit() ? "命中" : "未命中")
                    .append(" | ")
                    .append(result.getLatencyMs())
                    .append(" | ")
                    .append(escapeTable(result.getFailureReason()))
                    .append(" |\n");
        }
        return report.toString();
    }

    /**
     * 按需写出 Markdown 报告。
     *
     * <p>学习阶段经常需要比较多轮参数调整，把报告落到文件里可以形成可追踪证据，
     * 也方便后续粘贴到任务留痕或提交说明中。</p>
     */
    private void writeReportIfNeeded(RagEvalRequest request, RagEvalResponse response, long startedAt) {
        if (request.getReportPath() == null || request.getReportPath().isBlank()) {
            return;
        }
        Path reportPath = Path.of(request.getReportPath()).toAbsolutePath().normalize();
        try {
            if (reportPath.getParent() != null) {
                Files.createDirectories(reportPath.getParent());
            }
            Files.writeString(reportPath, response.getReportMarkdown(), StandardCharsets.UTF_8);
            response.setReportPath(reportPath.toString());
            response.getSteps().add(step("write_eval_report", "success", "评测报告已写入: " + reportPath, startedAt));
        } catch (IOException e) {
            throw new IllegalStateException("写入 RAG 评测报告失败: " + reportPath, e);
        }
    }

    private String sourceText(RagChunkHit hit) {
        RagSource source = hit.getSource();
        if (source == null) {
            return hit.getChunkId() + " " + hit.getTitle();
        }
        return String.join(" ",
                nullToEmpty(hit.getChunkId()),
                nullToEmpty(source.getChunkId()),
                nullToEmpty(source.getDocumentId()),
                nullToEmpty(source.getTitle()),
                nullToEmpty(source.getSourceUri()),
                nullToEmpty(source.getSectionTitle()));
    }

    private String contentText(RagChunkHit hit) {
        RagSource source = hit.getSource();
        return String.join(" ",
                nullToEmpty(hit.getTitle()),
                nullToEmpty(source == null ? null : source.getSectionTitle()),
                nullToEmpty(hit.getContentPreview()),
                nullToEmpty(hit.getContent()));
    }

    private String resolveCaseId(RagEvalCase evalCase) {
        return evalCase.getCaseId() == null || evalCase.getCaseId().isBlank()
                ? "case-" + RagHashUtilsHolder.shortQuestionId(evalCase.getQuestion())
                : evalCase.getCaseId();
    }

    private double rate(int count, int total) {
        return total == 0 ? 0.0d : Math.round((count * 10000.0d / total)) / 10000.0d;
    }

    private String escapeTable(String value) {
        return nullToEmpty(value).replace("|", "\\|").replace("\n", " ");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private RagTraceStep step(String name, String status, String detail, long startedAt) {
        RagTraceStep step = new RagTraceStep();
        step.setName(name);
        step.setStatus(status);
        step.setDetail(detail);
        step.setDurationMs(System.currentTimeMillis() - startedAt);
        return step;
    }

    /**
     * 避免在评测服务主流程里暴露 hash 实现细节，只用于缺省 caseId。
     */
    private static final class RagHashUtilsHolder {
        private static String shortQuestionId(String question) {
            return com.aidemo.rag.util.RagHashUtils.shortHash(question == null ? "" : question);
        }
    }
}
