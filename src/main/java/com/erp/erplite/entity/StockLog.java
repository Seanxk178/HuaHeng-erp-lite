package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 库存流水台账表
 */
@Data
@TableName("inv_stock_log")
public class StockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long goodsId; // 关联商品ID
    private Long warehouseId; // 关联仓库ID
    
    private java.math.BigDecimal beforeQuantity; // 变动前数量
    private java.math.BigDecimal changeQuantity; // 变动数量 (正数为入库，负数为出库)
    private java.math.BigDecimal afterQuantity; // 变动后数量
    
    private String relatedOrderNo; // 关联的单据号 (追溯凭证)
    private Integer type; // 变动类型: 1-采购入库, 2-销售出库, 3-盘点调整
    
    private Long createBy; // 操作人
    private Date createTime; // 变动时间
}
