package com.sunyujie.picture_backend.model.dto.user;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

/**
 * 登录请求
 * <p>
 * 规则与前端 Login.vue 保持一致。注意这里刻意不加字符集限制：登录页只要求 2 ~ 32 位，
 * 早期注册的账号可能包含其他字符，收紧会让存量用户登不进来。
 */
@Data
public class LoginRequest {

    /**
     * 账号：2 ~ 32 位
     */
    @NotBlank(message = "请输入账号")
    @Size(min = 2, max = 32, message = "账号长度需在 2 ~ 32 个字符之间")
    private String userAccount;

    /**
     * 密码：至少 6 位
     */
    @NotBlank(message = "请输入密码")
    @Size(min = 6, message = "密码至少 6 位")
    private String userPassword;
}
