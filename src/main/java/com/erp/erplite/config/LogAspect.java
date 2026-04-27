package com.erp.erplite.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.common.Log;
import com.erp.erplite.entity.SysLog;
import com.erp.erplite.entity.User;
import com.erp.erplite.mapper.SysLogMapper;
import com.erp.erplite.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 操作日志 AOP 切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysLogMapper sysLogMapper;
    private final UserMapper userMapper;

    // 定义切点：所有贴了 @Log 注解的方法都会被拦截
    @Pointcut("@annotation(com.erp.erplite.common.Log)")
    public void logPointCut() {}

    // 环绕通知：在方法执行前后都干点事
    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        // 1. 先让原本的业务方法去执行（比如执行添加商品的逻辑）
        Object result = point.proceed();
        long time = System.currentTimeMillis() - beginTime;

        // 2. 方法成功执行完后，记录日志
        try {
            saveSysLog(point, time);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
        return result;
    }

    private void saveSysLog(ProceedingJoinPoint joinPoint, long time) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SysLog sysLog = new SysLog();

        // 获取注解上的描述信息
        Log logAnnotation = method.getAnnotation(Log.class);
        if (logAnnotation != null) {
            sysLog.setOperation(logAnnotation.value());
        }

        // 获取调用的类名和方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        sysLog.setMethod(className + "." + methodName + "()");

        // 获取请求参数（转换为字符串简易保存）
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            sysLog.setParams(Arrays.toString(args));
        }

        // 获取当前 HTTP 请求，提取 IP 和 Token，找出是谁操作的
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            sysLog.setIp(request.getRemoteAddr());
            String token = request.getHeader("token");
            if (token != null) {
                // 根据 token 查出用户名
                QueryWrapper<User> query = new QueryWrapper<>();
                query.eq("token", token);
                User user = userMapper.selectOne(query);
                if (user != null) {
                    sysLog.setUsername(user.getUsername());
                }
            }
        }
        // 保存入库
        sysLogMapper.insert(sysLog);
    }
}