package com.sunyujie.picture_backend.model.dto.spaceUser;

import lombok.Data;

/**
 * 查询空间成员列表请求（先做简单列表，不分页）
 */
@Data
public class SpaceUserQueryRequest {

    /**
     * 空间 id
     */
    private Long spaceId;
}
