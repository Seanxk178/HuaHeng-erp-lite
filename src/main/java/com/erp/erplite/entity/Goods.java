package com.erp.erplite.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 商品实体类
 */
@Data
@TableName("base_goods") // 指定对应的数据库表名
public class Goods {

    @ExcelIgnore
    @TableId(type = IdType.AUTO) // 主键自增策略
    private Long id;

    /**
     * 商品唯一编码
     */
    @ExcelProperty("商品编码")
    private String code;

    /**
     * 商品名称
     */
    @ExcelProperty("商品名称")
    private String name;

    /**
     * 物料分类
     */
    @ExcelProperty("商品分类")
    private String category;

    /**
     * 品牌
     */
    @ExcelProperty("品牌")
    private String brand;

    /**
     * 计量单位
     */
    @ExcelProperty("计量单位")
    private String unit;

    /**
     * 规格型号
     */
    @ExcelProperty("规格型号")
    private String spec;

    /**
     * 全局默认指导价 (用于无头盘盈的成本初始化)
     */
    @ExcelProperty("默认指导价")
    private java.math.BigDecimal defaultPrice;

    /**
     * 商品图示网络路径
     */
    @ExcelIgnore
    private String imageUrl;

    /**
     * 状态: 1-启用, 0-停用
     */
    @ExcelIgnore
    private Integer status;

    /**
     * 创建时间
     */
    @ExcelIgnore
    private Date createTime;
}