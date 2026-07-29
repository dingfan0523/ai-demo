package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.model.EmbeddingVector;
import com.aidemo.rag.util.RagHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地 hash embedding 服务。
 *
 * <p>这是学习阶段的最小可运行 embedding 实现，不依赖外部模型 API。
 * 它会把中文字符/二元组、英文单词和数字 token hash 到固定维度向量中，再做 L2 归一化。
 * 这样可以先跑通“文本向量化 -> 相似度检索”的工程链路。</p>
 */
@Service
@RequiredArgsConstructor
public class LocalHashEmbeddingService implements EmbeddingService {

    private static final int DIMENSION = 128;

    private final RagProperties ragProperties;
    private final RagTextTokenizer tokenizer;

    @Override
    public String modelName() {
        return ragProperties.getEmbeddingModel();
    }

    @Override
    public EmbeddingVector embed(String text) {
        double[] values = new double[DIMENSION];
        for (String token : tokenizer.tokenize(text)) {
            int bucket = Math.floorMod(RagHashUtils.sha256(token).hashCode(), DIMENSION);
            values[bucket] += 1.0d;
        }
        normalize(values);

        EmbeddingVector vector = new EmbeddingVector();
        vector.setModel(modelName());
        vector.setDimension(DIMENSION);
        vector.setValues(toList(values));
        return vector;
    }

    @Override
    public List<EmbeddingVector> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        return texts.stream()
                .map(this::embed)
                .toList();
    }

    private void normalize(double[] values) {
        double sum = 0.0d;
        for (double value : values) {
            sum += value * value;
        }
        if (sum == 0.0d) {
            return;
        }
        double norm = Math.sqrt(sum);
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] / norm;
        }
    }

    private List<Double> toList(double[] values) {
        List<Double> result = new ArrayList<>(values.length);
        for (double value : values) {
            result.add(value);
        }
        return result;
    }
}
