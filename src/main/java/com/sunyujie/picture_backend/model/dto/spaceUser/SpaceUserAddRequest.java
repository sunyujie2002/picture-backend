package com.sunyujie.picture_backend.model.dto.spaceUser;

import lombok.Data;

/**
 * 邀请成员（新增空间用户关联）请求
 */
@Data
public class SpaceUserAddRequest {

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 被邀请用户 id
     */
    private Long userId;

    /**
     * 空间角色：仅可传 viewer / editor（admin 只能随空间创建产生）
     */
    private String spaceRole;
}
