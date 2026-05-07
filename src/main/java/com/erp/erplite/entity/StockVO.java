package com.erp.erplite.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存台账展示对象 (View Object)
 * 用于将多张表的数据组合在一起发给前端
 */
@Data
public class StockVO {
    // 忽略这个字段，不导出到 Excel
    @ExcelIgnore
    private Long goodsId;

    @ExcelProperty("商品编码")
    private String goodsCode;

    @ExcelProperty("仓库ID")
    private Long warehouseId;

    @ExcelProperty("商品名称")
    private String goodsName;

    @ExcelProperty("计量单位")
    private String unit;

    @ExcelProperty("当前库存数量")
    private java.math.BigDecimal quantity;

    @ExcelProperty("库存总成本(元)")
    private BigDecimal totalCost;

    @ExcelProperty("加权平均单价(元)")
    private BigDecimal avgPrice;
}