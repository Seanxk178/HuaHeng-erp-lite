package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.common.UserContext;
import com.erp.erplite.entity.*;
import com.erp.erplite.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysService {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final UserMapper userMapper;
    private final SysLogMapper sysLogMapper;

    private static final String SALT = "ERP_LITE_SALT";

    // ===================== 菜单权限 =====================

    /** 查询当前登录用户有权限的菜单 key 列表 */
    public List<String> getMyMenuKeys() {
        User user = UserContext.get();
        if ("ROLE_ADMIN".equals(user.getRole())) {
            return sysMenuMapper.selectList(null)
                    .stream().map(SysMenu::getMenuKey).toList();
        }
        return sysRoleMenuMapper.selectMenuKeysByRoleKey(user.getRole());
    }

    /** 查询全部角色 */
    public List<SysRole> getAllRoles() {
        return sysRoleMapper.selectList(null);
    }

    /** 查询全部菜单 */
    public List<SysMenu> getAllMenus() {
        return sysMenuMapper.selectList(null);
    }

    /** 查询某角色当前已有的菜单 id 列表 */
    public List<Long> getRoleMenuIds(Long roleId) {
        return sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    /** 保存角色的菜单权限（替换式覆盖） */
    @Transactional
    public void saveRoleMenus(Long roleId, List<Long> menuIds) {
        QueryWrapper<SysRoleMenu> delQuery = new QueryWrapper<>();
        delQuery.eq("role_id", roleId);
        sysRoleMenuMapper.delete(delQuery);
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            sysRoleMenuMapper.insert(rm);
        }
    }

    // ===================== 用户管理 =====================

    /** 查询全部用户（脱敏：清空密码和 token） */
    public List<User> getAllUsers() {
        List<User> users = userMapper.selectList(null);
        users.forEach(u -> { u.setPassword(null); u.setToken(null); });
        return users;
    }

    /** 修改用户角色 */
    public void updateUserRole(Long userId, String roleKey) {
        User u = new User();
        u.setId(userId);
        u.setRole(roleKey);
        userMapper.updateById(u);
    }

    /** 新增用户（密码自动 MD5+盐 加密） */
    public void createUser(String username, String password, String role) {
        QueryWrapper<User> check = new QueryWrapper<>();
        check.eq("username", username);
        if (userMapper.exists(check)) {
            throw new RuntimeException("用户名「" + username + "」已存在，请换一个");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(encryptPassword(password));
        u.setRole(role);
        u.setCreateTime(new Date());
        userMapper.insert(u);
    }

    /** 修改用户名（唯一性校验） */
    public void updateUsername(Long userId, String newUsername) {
        QueryWrapper<User> check = new QueryWrapper<>();
        check.eq("username", newUsername);
        if (userMapper.exists(check)) {
            throw new RuntimeException("用户名「" + newUsername + "」已被占用");
        }
        User u = new User();
        u.setId(userId);
        u.setUsername(newUsername);
        userMapper.updateById(u);
    }

    /** 重置密码（同时清空 token，强制该用户重新登录） */
    public void resetPassword(Long userId, String newPassword) {
        User u = new User();
        u.setId(userId);
        u.setPassword(encryptPassword(newPassword));
        u.setToken("");  // 清空旧 token，强制登出
        userMapper.updateById(u);
    }

    /** 删除用户（禁止删除自身） */
    public void deleteUser(Long userId) {
        User self = UserContext.get();
        if (self.getId().equals(userId)) {
            throw new RuntimeException("不能删除当前正在登录的账号");
        }
        userMapper.deleteById(userId);
    }

    /** 查询操作日志 (带分页) */
    public com.baomidou.mybatisplus.core.metadata.IPage<SysLog> getLogs(int pageNum, int pageSize) {
        return sysLogMapper.getLogList(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize));
    }

    // ===================== 内部工具 =====================

    private String encryptPassword(String rawPassword) {
        return DigestUtils.md5DigestAsHex((rawPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }
}
