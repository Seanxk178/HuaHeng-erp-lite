package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 商品实体类
 */
@Data
@TableName("base_goods") // 指定对应的数据库表名
public class Goods {

    @TableId(type = IdType.AUTO) // 主键自增策略
    private Long id;

    /**
     * 商品唯一编码
     */
    private String code;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 创建时间
     */
    private Date createTime;
}