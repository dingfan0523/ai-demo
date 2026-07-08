package com.aidemo.common;

import lombok.Data;

/**
 * 统一接口响应结果
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> {

    /** 状态码：200 为成功 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
