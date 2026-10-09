package com.sunyujie.picture_backend.model.dto.user;

import lombok.Data;

/**
 * 用户修改自己信息请求：仅允许修改昵称、头像、简介
 * <p>
 * id、账号、密码、角色等字段均不接受，目标用户由服务端从登录态确定。
 */
@Data
public class UpdateMyInfoRequest {

    /**
     * 用户昵称（不传表示不更新；传值不允许为纯空格）
     */
    private String userName;

    /**
     * 用户头像（不传表示不更新）
     */
    private String userAvatar;

    /**
     * 用户简介（不传表示不更新；传空字符串表示清空）
     */
    private String userProfile;
}
