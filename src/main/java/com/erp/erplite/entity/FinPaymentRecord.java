package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 收付款流水表
 */
@Data
@TableName("fin_payment_record")
public class FinPaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long accountId; // 关联的财务账户表(fin_account) ID
    private Long partnerId; // 关联客商ID
    
    private Integer type; // 支付类型: 1-付款(出账), 2-收款(入账)
    private BigDecimal amount; // 实际收付金额
    
    private Integer paymentMethod; // 支付方式: 1-银行转账, 2-微信, 3-支付宝, 4-现金
    private String transactionNo; // 第三方交易流水号
    
    private Long createBy; // 经办人
    private Date paymentTime; // 实际收付款时间
}
