package com.sunyujie.picture_backend.model.dto.spaceUser;

import lombok.Data;

/**
 * 移除空间成员请求
 */
@Data
public class SpaceUserRemoveRequest {

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 被移除用户 id
     */
    private Long userId;
}
