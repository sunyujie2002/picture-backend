package com.sunyujie.picture_backend.model.dto.user;

import java.time.LocalDateTime;

import com.sunyujie.picture_backend.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询请求（列表/分页查询）
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class QueryUserRequest extends PageRequest {

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
