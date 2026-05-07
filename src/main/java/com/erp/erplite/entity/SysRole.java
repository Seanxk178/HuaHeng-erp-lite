package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 角色唯一标识，如 ROLE_ADMIN */
    private String roleKey;
    /** 可读名称，如 系统管理员 */
    private String roleName;
}
