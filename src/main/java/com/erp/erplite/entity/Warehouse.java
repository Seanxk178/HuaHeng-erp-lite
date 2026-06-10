package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("base_warehouse")
public class Warehouse {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer status; // 状态: 1-启用, 0-停用
    private Date createTime;
}
