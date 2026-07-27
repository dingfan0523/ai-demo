package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagChunkHit;

import java.util.List;

/**
 * RAG 重排服务。
 *
 * <p>负责把初筛候选重新排序。第一阶段建议使用可解释规则，后续再替换为模型化 rerank。</p>
 */
public interface RerankService {

    /**
     * 对候选 chunk 进行重排。
     *
     * @param query 用户查询
     * @param candidates 初筛候选
     * @param limit 返回数量
     * @return 重排后的候选
     */
    List<RagChunkHit> rerank(String query, List<RagChunkHit> candidates, int limit);
}
