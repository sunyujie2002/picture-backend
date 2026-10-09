package com.sunyujie.picture_backend.model.dto.spaceUser;

import lombok.Data;

/**
 * 修改空间成员角色请求
 */
@Data
public class SpaceUserUpdateRequest {

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 被修改角色的用户 id
     */
    private Long userId;

    /**
     * 目标空间角色：仅可传 viewer / editor（admin 只能随空间创建产生，不可修改得到）
     */
    private String spaceRole;
}
