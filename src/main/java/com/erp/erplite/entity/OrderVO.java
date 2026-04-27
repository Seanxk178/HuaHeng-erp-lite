package com.erp.erplite.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderVO {
    private Long orderId;
    private String orderNo;
    private String partnerName; // 供应商名称
    private String goodsName;   // 商品名称
    private Integer quantity;   // 数量
    private BigDecimal totalAmount; // 总金额
    private Integer status;     // 审核状态
    private Date createTime;
}