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
     * 捕获所有 Exception 异常
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

        // 1. 打印完整的错误日志到控制台，方便开发排查
        log.error("系统发生异常，异常信息: ", e);

        // 2. 封装错误信息，返回给前端统一的 JSON 格式
        // TODO: 后期可以根据不同的自定义异常（如业务异常、校验异常）做更细致的分类处理
        return Result.error("系统异常，请联系管理员: " + e.getMessage());
    }
}