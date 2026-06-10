package com.erp.erplite.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 拦截系统中所有 Controller 抛出的异常，统一封装为 Result.error() 返回给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * 业务异常属于已知异常，只打印普通提示，不需要打印长堆栈，避免污染日志
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务拦截: {}", e.getMessage());
        return Result.error(e.getCode() != null ? e.getCode() : 500, e.getMessage());
    }

    /**
     * 捕获所有未知 Exception 异常
     *
     * @param e 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 如果是浏览器请求 favicon.ico 找不到，直接过滤掉，避免打印无用的长堆栈
        if (e instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
            return Result.error(404, "资源未找到: " + e.getMessage());
        }

        // 1. 打印完整的错误日志到控制台，方便开发排查（未知异常必须打印堆栈）
        log.error("系统发生异常，异常信息: ", e);

        // 2. 封装错误信息，返回给前端统一的 JSON 格式
        return Result.error("系统繁忙，请稍后再试或联系管理员");
    }
}