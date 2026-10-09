package com.sunyujie.picture_backend.model.dto.user;

import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class UpdatePasswordRequest {

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
