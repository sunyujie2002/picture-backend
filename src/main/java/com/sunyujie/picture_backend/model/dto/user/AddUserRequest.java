package com.sunyujie.picture_backend.model.dto.user;

import lombok.Data;

@Data
public class AddUserRequest {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户角色：user/admin
     */
    private String userRole;
}
