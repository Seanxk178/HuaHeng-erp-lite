package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.entity.User;
import com.erp.erplite.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 用户登录逻辑
     */
    /**
     * 用户登录逻辑 (带密码平滑升级加密)
     */
    public com.erp.erplite.entity.LoginVO login(String username, String password) {
        log.info("用户尝试登录: {}", username);

        // 1. 根据用户名查询用户
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        query.eq("username", username);
        User user = userMapper.selectOne(query);

        if (user == null) {
            throw new RuntimeException("账号或密码错误！"); // 安全防范：不要明确提示"用户不存在"
        }

        // 2. 计算当前输入密码的 MD5 密文 (加固盐值 "ERP_LITE_SALT")
        // 使用 Spring 自带的工具类，无需额外引入依赖
        String salt = "ERP_LITE_SALT";
        String encryptedPassword = org.springframework.util.DigestUtils.md5DigestAsHex((password + salt).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 3. 密码比对与无缝升级逻辑
        boolean isLoginSuccess = false;

        if (user.getPassword().equals(password)) {
            // 情况A：数据库里存的还是明文密码（历史遗留数据）。比对成功后，立刻帮他升级为密文！
            log.info("检测到明文密码，正在为用户 {} 升级为密文存储", username);
            user.setPassword(encryptedPassword);
            isLoginSuccess = true;
        } else if (user.getPassword().equals(encryptedPassword)) {
            // 情况B：数据库里已经是密文了，正常比对密文
            isLoginSuccess = true;
        }

        if (!isLoginSuccess) {
            throw new RuntimeException("账号或密码错误！");
        }

        // 4. 生成 Token
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        user.setToken(token);

        // 5. 更新用户信息（可能包含了升级后的密文密码 和 新的 Token）
        userMapper.updateById(user);

        log.info("用户 {} 登录成功", username);

        // 6. 封装返回值给前端
        com.erp.erplite.entity.LoginVO vo = new com.erp.erplite.entity.LoginVO();
        vo.setToken(token);
        vo.setRole(user.getRole());
        vo.setUsername(user.getUsername());
        return vo;
    }
}