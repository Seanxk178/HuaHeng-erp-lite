package com.erp.erplite.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class FinAccountVO {
    private Long accountId;      // 账款ID
    private String partnerName;  // 客户/供应商名称
    private String orderNo;      // 关联的业务单号
    private Integer type;        // 1-应付(欠供应商), 2-应收(客户欠我)
    private BigDecimal amount;   // 应结总额
    private BigDecimal paidAmount; // 已结金额
    private Integer status;      // 0-未结清, 1-已结清
    private Date createTime;     // 账款产生时间
}