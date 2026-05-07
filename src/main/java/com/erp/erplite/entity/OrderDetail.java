package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("doc_order_detail")
public class OrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long goodsId;
    private java.math.BigDecimal quantity;
    private java.math.BigDecimal unitPrice;
    private java.math.BigDecimal totalAmount;
    private java.math.BigDecimal costAmount;
    private String remark; // 单行商品备注
}