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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    // 轻量级本地缓存，用于存储 Token 验证结果，降低数据库 IO 压力
    private static final Map<String, CacheEntry> tokenCache = new ConcurrentHashMap<>();

    // 暴露给外部调用，用于踢人下线或重置 token 时清理缓存
    public static void invalidateToken(String token) {
        if (token != null) {
            tokenCache.remove(token);
        }
    }

    private static class CacheEntry {
        User user;
        long cacheTime;
        CacheEntry(User user, long cacheTime) {
            this.user = user;
            this.cacheTime = cacheTime;
        }
    }

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

        // 2. 根据 token 获取用户 (优先读缓存)
        User user = null;
        long now = System.currentTimeMillis();
        CacheEntry entry = tokenCache.get(token);
        
        // 缓存有效期设为 5 分钟 (300000 毫秒)
        if (entry != null && (now - entry.cacheTime) < 300000) {
            user = entry.user;
        } else {
            // 缓存失效或不存在，查数据库
            QueryWrapper<User> query = new QueryWrapper<>();
            query.eq("token", token);
            user = userMapper.selectOne(query);
            
            if (user != null) {
                tokenCache.put(token, new CacheEntry(user, now));
            }
        }

        if (user == null) {
            tokenCache.remove(token); // 无效 token 从缓存清理
            return forbidden(response, 401, "Token 无效或已过期，请重新登录");
        }

        // 3. 校验 Token 是否已过期
        if (user.getTokenExpireTime() != null && user.getTokenExpireTime().before(new java.util.Date())) {
            // 清空过期 Token，强制下次重新登录
            user.setToken("");
            user.setTokenExpireTime(null);
            userMapper.updateById(user);
            tokenCache.remove(token); // 同时清理本地缓存
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
