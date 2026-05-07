package com.erp.erplite.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.common.RequireRole;
import com.erp.erplite.common.UserContext;
import com.erp.erplite.entity.User;
import com.erp.erplite.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 如果不是映射到方法直接通过 (比如静态资源)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 0. 放行所有的 OPTIONS 跨域预检请求
        if (org.springframework.http.HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        // 1. 从请求头获取 token
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            return forbidden(response, 401, "请先登录");
        }

        // 2. 根据 token 查询用户
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("token", token);
        User user = userMapper.selectOne(query);

        if (user == null) {
            return forbidden(response, 401, "Token 无效或已过期，请重新登录");
        }

        // 3. 校验 Token 是否已过期
        if (user.getTokenExpireTime() != null && user.getTokenExpireTime().before(new java.util.Date())) {
            // 清空过期 Token，强制下次重新登录
            user.setToken("");
            user.setTokenExpireTime(null);
            userMapper.updateById(user);
            return forbidden(response, 401, "登录已过期，请重新登录");
        }

        // 3. 将用户存入 ThreadLocal
        UserContext.set(user);

        // 4. 权限校验
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 优先获取方法上的注解，如果没有则获取类上的注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        if (requireRole != null) {
            String currentRole = user.getRole();
            if (currentRole != null && currentRole.startsWith("ROLE_")) {
                currentRole = currentRole.substring(5).toLowerCase();
            }

            String[] allowedRoles = requireRole.value();

            // "admin" 默认拥有所有权限
            if (!"admin".equals(currentRole) && !Arrays.asList(allowedRoles).contains(currentRole)) {
                return forbidden(response, 403, "没有权限访问此接口");
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        // 请求结束，清除 ThreadLocal，防止内存泄漏
        UserContext.remove();
    }

    private boolean forbidden(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(200); // 业务状态码用200，内容返回报错
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\": %d, \"message\": \"%s\", \"data\": null}", code, msg));
        return false;
    }
}
