package com.aidemo.rag.security;

/**
 * Prompt Injection 防护接口。
 *
 * <p>用于识别文档中可能试图覆盖系统指令的危险内容。文档内容只能作为参考资料，
 * 不能被当作新的系统指令执行。</p>
 */
public interface PromptInjectionGuard {

    /**
     * 判断内容是否存在明显 prompt injection 风险。
     *
     * @param content 待检查内容
     * @return true 表示疑似不安全
     */
    boolean looksUnsafe(String content);
}
