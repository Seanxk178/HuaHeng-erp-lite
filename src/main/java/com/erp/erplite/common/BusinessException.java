package com.erp.erplite.common;

/**
 * 业务异常类
 * 用于在业务逻辑中抛出已知的业务错误，避免与系统崩溃异常混淆
 */
public class BusinessException extends RuntimeException {

    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500; // 默认业务错误码
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
