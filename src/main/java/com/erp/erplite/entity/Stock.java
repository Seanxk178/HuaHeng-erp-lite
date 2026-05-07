package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.util.Date;

@Data
@TableName("inv_stock")
public class Stock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long goodsId;
    private Long warehouseId; // 仓库ID
    private java.math.BigDecimal quantity;

    @Version // 乐观锁版本号，防止并发超卖的关键
    private Integer version;

    private Date updateTime;

    private java.math.BigDecimal totalCost;
}