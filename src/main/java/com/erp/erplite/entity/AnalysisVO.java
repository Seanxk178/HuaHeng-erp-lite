package com.erp.erplite.entity;
import lombok.Data;

/**
 * 账龄/库龄 通用分析对象
 */
@Data
public class AnalysisVO {
    private String name;    // 商品名称 或 客户名称
    private String code;    // 商品编码 或 单据号
    private Integer days;   // 算出来的年龄（天数）
    private String level;   // 危险评级 (如: "健康", "警告", "危险")
    // 这里用泛型或者直接存字符串/数字，为了简单直观，我们存描述
    private String description; // 如 "10个库存", "欠款 500元"
}