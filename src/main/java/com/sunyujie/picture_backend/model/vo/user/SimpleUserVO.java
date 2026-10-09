package com.sunyujie.picture_backend.model.vo.user;

import java.io.Serializable;

import lombok.Data;

/**
 * 用户简要信息 VO：只暴露辨识用户所必需的 4 个字段。
 * <p>
 * 使用场景：
 * 1. 团队空间邀请成员时的用户搜索结果；
 * <p>
 * 平台角色（userRole）、简介、时间等均不对外暴露。
 */
@Data
public class SimpleUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 id
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
}
