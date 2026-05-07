package com.erp.erplite.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.lang.NonNull;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${erp.upload.image-path}")
    private String imagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 当浏览器请求 /images/** 时，Spring Boot 会自动去本地硬盘的 imagePath 目录下找文件
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imagePath);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")        // 拦截所有路径
                .excludePathPatterns(
                        "/user/login",         // 排除登录
                        "/error",              // 排除错误页面
                        "/**/*.html",          // 排除静态资源
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.map",
                        "/**/*.svg",
                        "/**/*.ico",
                        "/images/**",
                        "/.well-known/**"
                );
    }
}
