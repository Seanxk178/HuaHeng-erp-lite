package com.erp.erplite.entity;
import lombok.Data;
@Data
public class LoginVO {
    private String token;
    private String role;     // 返回角色标识给前端
    private String username; // 返回用户名用于页面右上角展示
}