package com.erp.erplite.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.entity.User;
import com.erp.erplite.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;

    @Override
    public void run(String... args) throws Exception {
        // 自动为同事创建测试账号
        createUserIfNotExist("admin", "123456", "ROLE_ADMIN");
        createUserIfNotExist("sales", "123456", "ROLE_PURCHASE");
        createUserIfNotExist("finance", "123456", "ROLE_FINANCE");
        // 如果你需要仓库角色，前端以后适配了菜单可以用这个
        createUserIfNotExist("warehouse", "123456", "ROLE_WAREHOUSE");
    }

    private void createUserIfNotExist(String username, String password, String role) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", username);
        if (!userMapper.exists(query)) {
            User user = new User();
            user.setUsername(username);
            // 这里直接存明文 123456，
            // 当他们第一次用 123456 登录时，UserService 会自动帮他们升级成 MD5 密文！
            user.setPassword(password);
            user.setRole(role);
            user.setCreateTime(new Date());
            userMapper.insert(user);
            log.info("已自动初始化测试账号 -> 账号: {}, 密码: {}, 角色: {}", username, password, role);
        }
    }
}
