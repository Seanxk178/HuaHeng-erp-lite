package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("doc_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Integer type; // 1-入库单，2-出库单
    private Long createBy;
    private Date createTime;
    private Long partnerId;
    private Integer status; // 0-待审核, 1-已审核

}