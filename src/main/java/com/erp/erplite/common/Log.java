package com.erp.erplite.common;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义操作日志注解
 */
@Target(ElementType.METHOD) // 表明这个注解只能贴在方法上
@Retention(RetentionPolicy.RUNTIME) // 表明在运行时生效
public @interface Log {
    // 记录操作的描述信息，例如 "新增了往来单位"
    String value() default "";
}