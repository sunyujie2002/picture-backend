package com.sunyujie.picture_backend.model.dto.user;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 用户登录态 DTO（存 Redis、放 ThreadLocal 的当前登录用户信息，脱敏不含密码）
 */
@Data
public class UserDTO {

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 登录态 token
     */
    private String token;
}
