package com.erp.erplite.entity;

import lombok.Data;
import java.util.List;

@Data
public class LoginVO {
    private String token;
    private String role;          // 返回角色标识给前端（保持向前兼容）
    private String username;      // 返回用户名用于页面右上角展示
    private List<String> menuKeys; // 该用户有权访问的菜单 key 列表（RBAC 动态权限）
}