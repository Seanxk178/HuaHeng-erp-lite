package com.erp.erplite.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("fin_account")
public class FinAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long partnerId;
    private Long orderId;
    private Integer type; // 1-应付(采购), 2-应收(销售)
    private BigDecimal amount;
    private Integer status; // 0-未结清, 1-已结清
    private Date createTime;
}