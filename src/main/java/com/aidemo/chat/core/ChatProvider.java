package com.aidemo.chat.core;

import com.aidemo.chat.dto.AiReply;

import java.util.function.Consumer;

/**
 * AI 模型提供者策略接口
 *
 * <p>每个模型（ChatGPT、DeepSeek 等）实现此接口，封装该模型的具体能力差异。
 * 普通问答和流式问答由抽象基类统一实现，子类只需覆盖结构化问答的差异逻辑。</p>
 */
public interface ChatProvider {

    /**
     * 获取提供者名称
     */
    String getName();

    /**
     * 普通问答（同步纯文本）
     */
    String chatPlain(String message);

    /**
     * 流式问答
     *
     * @param message   用户消息
     * @param onToken   每个 token 到达时的回调
     * @param onComplete 流式输出完成时的回调
     * @param onError   发生异常时的回调
     */
    void chatStream(String message,
                    Consumer<String> onToken,
                    Runnable onComplete,
                    Consumer<Throwable> onError);

    /**
     * 结构化问答（返回 AiReply 实体）
     */
    AiReply chatStructured(String message);
}
