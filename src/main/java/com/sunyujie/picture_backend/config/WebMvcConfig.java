package com.sunyujie.picture_backend.config;

import javax.annotation.Resource;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sunyujie.picture_backend.interceptor.LoginInterceptor;

/**
 * Web MVC 配置：注册全局登录拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录、注册接口不需要 token
                        "/user/login",
                        "/user/register",
                        // 接口文档（Knife4j / Swagger）
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico",
                        // 静态首页
                        "/",
                        "/index.html"
                );
    }
}
