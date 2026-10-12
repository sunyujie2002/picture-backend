package com.sunyujie.picture_backend.model.dto.user;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Data;

/**
 * 注册请求
 * <p>
 * 校验规则与前端 Register.vue 的表单规则逐条对齐：前端拦得住的，后端也要拦得住，
 * 因为接口可以被绕过表单直接调用。文案也保持一致，用户收到的提示不会前后矛盾。
 */
@Data
public class RegisterRequest {

    /**
     * 账号：4 ~ 32 位字母、数字、下划线
     */
    @NotBlank(message = "请输入账号")
    @Size(min = 4, max = 32, message = "账号长度需在 4 ~ 32 个字符之间")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String userAccount;

    /**
     * 密码：至少 6 位
     */
    @NotBlank(message = "请输入密码")
    @Size(min = 6, message = "密码至少 6 位")
    private String userPassword;

    /**
     * 确认密码：非空；与密码是否一致由 service 比较（那里还要兼顾空值判断）
     */
    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;
}
