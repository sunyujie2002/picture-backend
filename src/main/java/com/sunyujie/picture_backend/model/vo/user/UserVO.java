package com.sunyujie.picture_backend.model.vo.user;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 用户视图对象（返回给前端的用户信息，脱敏不含密码）
 */
@Data
public class UserVO {

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
     * 创建时间
     */
    private LocalDateTime createTime;
}
