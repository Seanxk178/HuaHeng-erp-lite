package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.common.SystemConstants;
import com.erp.erplite.entity.User;
import com.erp.erplite.mapper.SysMenuMapper;
import com.erp.erplite.mapper.SysRoleMenuMapper;
import com.erp.erplite.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    // BCrypt 自带动态盐，安全性极高
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * 用户登录逻辑 (带密码平滑升级至 BCrypt 加密)
     */
    public com.erp.erplite.entity.LoginVO login(String username, String password) {
        log.info("用户尝试登录: {}", username);

        // 1. 根据用户名查询用户
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", username);
        User user = userMapper.selectOne(query);

        if (user == null) {
            throw new com.erp.erplite.common.BusinessException("账号或密码错误！"); // 安全防范：不要明确提示"用户不存在"
        }

        // 2. 密码比对与无缝升级逻辑 (目标终态：所有密码均为 BCrypt)
        boolean isLoginSuccess = false;

        // 尝试按 BCrypt 匹配（如果已经是新系统格式）
        if (BCRYPT.matches(password, user.getPassword())) {
            isLoginSuccess = true;
        } else {
            // 兼容逻辑：判断是否是老系统的明文或 MD5
            String oldMd5Password = org.springframework.util.DigestUtils.md5DigestAsHex(
                    (password + SystemConstants.OLD_MD5_SALT).getBytes(java.nio.charset.StandardCharsets.UTF_8));

            if (user.getPassword().equals(password)) {
                log.info("检测到明文密码，正在为用户 {} 升级为 BCrypt 密文存储", username);
                user.setPassword(BCRYPT.encode(password));
                isLoginSuccess = true;
            } else if (user.getPassword().equals(oldMd5Password)) {
                log.info("检测到弱 MD5 密码，正在为用户 {} 升级为 BCrypt 密文存储", username);
                user.setPassword(BCRYPT.encode(password));
                isLoginSuccess = true;
            }
        }

        if (!isLoginSuccess) {
            throw new com.erp.erplite.common.BusinessException("账号或密码错误！");
        }

        // 3. 踢出旧设备登录：如果当前用户有旧 token，从本地缓存剔除，实现秒级互踢
        String oldToken = user.getToken();
        if (oldToken != null && !oldToken.isEmpty()) {
            com.erp.erplite.config.AuthInterceptor.invalidateToken(oldToken);
        }

        // 生成新 Token，并设置 8 小时有效期
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setToken(token);
        // 当前时间 + 8小时（8 * 60 * 60 * 1000 毫秒）
        user.setTokenExpireTime(new java.util.Date(System.currentTimeMillis() + 8L * 3600 * 1000));

        // 4. 更新用户信息（升级后的密文密码 + 新 Token + 过期时间）
        userMapper.updateById(user);

        log.info("用户 {} 登录成功", username);

        // 5. 封装返回值给前端
        com.erp.erplite.entity.LoginVO vo = new com.erp.erplite.entity.LoginVO();
        vo.setToken(token);
        vo.setRole(user.getRole());
        vo.setUsername(user.getUsername());

        // 6. 查询该用户角色对应的菜单权限
        List<String> menuKeys;
        if (SystemConstants.ROLE_ADMIN.equals(user.getRole())) {
            menuKeys = sysMenuMapper.selectList(null)
                    .stream().map(com.erp.erplite.entity.SysMenu::getMenuKey).toList();
        } else {
            String roleKey = user.getRole();
            // 直接使用数据库存储的 roleKey 进行查询，不再强制转大写+ROLE_前缀
            // 兼容已有代码逻辑：如果它有 ROLE_，那么查出来的可能匹配；如果是 purchase_manager 也能精确匹配
            menuKeys = sysRoleMenuMapper.selectMenuKeysByRoleKey(roleKey);
        }
        vo.setMenuKeys(menuKeys);

        return vo;
    }
}