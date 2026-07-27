package com.aidemo.rag.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * RAG hash 工具。
 *
 * <p>当前用于生成稳定文档 ID、内容 hash 和 chunk hash。hash 是入库去重和增量更新的基础，
 * 后续接持久化存储时也可以沿用这套规则。</p>
 */
public final class RagHashUtils {

    private RagHashUtils() {
    }

    /**
     * 计算文本的 SHA-256 hash。
     *
     * @param value 输入文本
     * @return 十六进制 hash
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    /**
     * 返回较短的 hash 前缀，适合用于学习阶段可读 ID。
     *
     * @param value 输入文本
     * @return 12 位 hash 前缀
     */
    public static String shortHash(String value) {
        return sha256(value).substring(0, 12);
    }
}
