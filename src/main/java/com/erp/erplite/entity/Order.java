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
    private Integer status; // 状态：0-草稿, 1-待审核, 2-已审核, 3-作废, 4-已驳回
    private java.math.BigDecimal totalAmount; // 订单总金额（冗余字段，提升统计性能）
    private Long warehouseId; // 仓库ID
    
    @com.baomidou.mybatisplus.annotation.Version
    private Integer version; // 乐观锁版本号
    
    private String contractNo; // 关联外部合同编号
    private String remark; // 整单备注

}