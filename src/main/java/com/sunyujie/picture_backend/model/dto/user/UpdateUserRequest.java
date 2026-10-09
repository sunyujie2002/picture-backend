package com.sunyujie.picture_backend.model.dto.user;

import lombok.Data;

/**
 * 更新用户请求参数
 */
@Data
public class UpdateUserRequest {
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称（公开展示，允许管理员治理违规昵称）
     */
    private String userName;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 用户简介
     */
    private String userProfile;
}
