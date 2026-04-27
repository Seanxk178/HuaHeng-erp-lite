package com.erp.erplite.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("base_partner")
public class Partner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer type; // 1-供应商, 2-客户
    private String contact;
    private String phone;
    private Date createTime;
}