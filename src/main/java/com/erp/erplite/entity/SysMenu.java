package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 菜单唯一标识，与前端 currentMenu 的值完全一致，如 inbound */
    private String menuKey;
    /** 可读名称，如 采购入库 */
    private String menuName;
}
