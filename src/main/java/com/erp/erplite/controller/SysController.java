package com.erp.erplite.controller;

import com.erp.erplite.common.RequireRole;
import com.erp.erplite.common.Result;
import com.erp.erplite.entity.SysMenu;
import com.erp.erplite.entity.SysRole;
import com.erp.erplite.entity.User;
import com.erp.erplite.service.SysService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理 API：角色、菜单权限、用户管理
 */
@RestController
@RequestMapping("/sys")
@RequiredArgsConstructor
public class SysController {

    private final SysService sysService;

    /** 获取当前登录用户有权访问的菜单 key 列表（所有已登录用户均可调用） */
    @GetMapping("/my-menus")
    public Result<List<String>> getMyMenus() {
        return Result.success(sysService.getMyMenuKeys());
    }

    /** 获取全部角色列表（仅管理员） */
    @RequireRole("admin")
    @GetMapping("/roles")
    public Result<List<SysRole>> getRoles() {
        return Result.success(sysService.getAllRoles());
    }

    /** 获取全部菜单列表（仅管理员） */
    @RequireRole("admin")
    @GetMapping("/menu/all")
    public Result<List<SysMenu>> getAllMenus() {
        return Result.success(sysService.getAllMenus());
    }

    /** 获取某角色已配置的菜单 id 列表（仅管理员） */
    @RequireRole("admin")
    @GetMapping("/role/menus/{roleId}")
    public Result<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        return Result.success(sysService.getRoleMenuIds(roleId));
    }

    /** 保存角色的菜单权限（仅管理员） */
    @RequireRole("admin")
    @PostMapping("/role/menus")
    public Result<String> saveRoleMenus(@RequestBody RoleMenuParam param) {
        sysService.saveRoleMenus(param.getRoleId(), param.getMenuIds());
        return Result.success("权限保存成功");
    }

    /** 获取全部用户列表（仅管理员） */
    @RequireRole("admin")
    @GetMapping("/users")
    public Result<List<User>> getUsers() {
        return Result.success(sysService.getAllUsers());
    }

    /** 修改用户角色（仅管理员） */
    @RequireRole("admin")
    @PutMapping("/user/role")
    public Result<String> updateUserRole(@RequestBody UserRoleParam param) {
        sysService.updateUserRole(param.getUserId(), param.getRoleKey());
        return Result.success("角色修改成功");
    }

    /** 新增用户（仅管理员） */
    @RequireRole("admin")
    @PostMapping("/user")
    public Result<String> createUser(@RequestBody CreateUserParam param) {
        sysService.createUser(param.getUsername(), param.getPassword(), param.getRole());
        return Result.success("用户创建成功");
    }

    /** 修改用户名（仅管理员） */
    @RequireRole("admin")
    @PutMapping("/user/username")
    public Result<String> updateUsername(@RequestBody UpdateUsernameParam param) {
        sysService.updateUsername(param.getUserId(), param.getNewUsername());
        return Result.success("用户名修改成功");
    }

    /** 重置密码（仅管理员） */
    @RequireRole("admin")
    @PutMapping("/user/password")
    public Result<String> resetPassword(@RequestBody ResetPasswordParam param) {
        sysService.resetPassword(param.getUserId(), param.getNewPassword());
        return Result.success("密码重置成功，该用户下次登录时需使用新密码");
    }

    /** 删除用户（禁止删除自身） */
    @RequireRole("admin")
    @DeleteMapping("/user/{userId}")
    public Result<String> deleteUser(@PathVariable Long userId) {
        sysService.deleteUser(userId);
        return Result.success("用户已删除");
    }

    /** 获取操作日志（仅管理员） */
    @RequireRole("admin")
    @GetMapping("/logs")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<com.erp.erplite.entity.SysLog>> getLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(sysService.getLogs(pageNum, pageSize));
    }

    @Data
    public static class RoleMenuParam {
        private Long roleId;
        private List<Long> menuIds;
    }

    @Data
    public static class UserRoleParam {
        private Long userId;
        private String roleKey;
    }

    @Data
    public static class CreateUserParam {
        private String username;
        private String password;
        private String role;
    }

    @Data
    public static class UpdateUsernameParam {
        private Long userId;
        private String newUsername;
    }

    @Data
    public static class ResetPasswordParam {
        private Long userId;
        private String newPassword;
    }
}
