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
                        // 公共图库对游客开放：只看「已通过审核且不属于任何空间」的图片，
                        // 接口内部不依赖登录态（UserContext.getLoginUserId() 取不到时为 null，
                        // 只会拦住未过审图片的匿名访问），其余接口仍全部要求登录
                        "/picture/home/queryList",
                        "/category/queryList",
                        "/tag/queryList",
                        "/picture/queryById",
                        "/picture/search/picture",
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
