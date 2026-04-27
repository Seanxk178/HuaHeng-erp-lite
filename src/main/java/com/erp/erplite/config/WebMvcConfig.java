package com.erp.erplite.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置类 (注册拦截器)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/index.html",
                        "/",
                        "/**/*.js",
                        "/**/*.css",
                        "/**/*.ico",
                        "/error",
                        "/.well-known/**" // <--- 加上这一行，屏蔽 Chrome 的无聊请求
                );
    }
}