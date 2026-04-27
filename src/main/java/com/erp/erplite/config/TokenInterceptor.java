package com.erp.erplite.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.entity.User;
import com.erp.erplite.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 核心安全拦截器
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 放行所有的 OPTIONS 跨域预检请求
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        // 2. 从 HTTP 请求头中获取 token
        String token = request.getHeader("token");

        // 3. 如果没带 token，直接抛出特殊的未登录异常
        if (token == null || token.trim().isEmpty()) {
            log.warn("拦截到非法请求，未携带Token, 接口: {}", request.getRequestURI());
            // 抛出约定好的错误信息，前端只要看到 "NOT_LOGIN" 就会跳转到登录页
            throw new RuntimeException("NOT_LOGIN");
        }

        // 4. 去数据库查这个 token 存不存在
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("token", token);
        boolean exists = userMapper.exists(query);

        if (!exists) {
            log.warn("拦截到伪造或已过期的Token: {}", token);
            throw new RuntimeException("NOT_LOGIN");
        }

        // 5. 校验通过，放行请求去执行具体的 Controller
        return true;
    }
}