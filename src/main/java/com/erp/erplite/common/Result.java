package com.erp.erplite.common;

import lombok.Data;

/**
 * 统一 API 响应结果封装类
 * 所有的 Controller 接口都必须返回这个类，保证前后端交互格式一致
 *
 * @param <T> 泛型，代表具体返回的数据类型
 */
@Data
public class Result<T> {

    /**
     * 业务状态码：200-成功，500-失败
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 具体的响应数据
     */
    private T data;

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /**
     * 成功返回（带数据）
     *
     * @param data 业务数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 失败返回
     *
     * @param message 错误提示信息
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}