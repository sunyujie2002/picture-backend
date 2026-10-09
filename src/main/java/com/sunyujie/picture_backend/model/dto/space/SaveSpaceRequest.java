package com.sunyujie.picture_backend.model.dto.space;

import lombok.Data;

/**
 * 保存空间请求
 */
@Data
public class SaveSpaceRequest {

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间类型：0-个人空间 1-团队空间（不传默认个人空间）
     */
    private Integer spaceType;

    /**
     * 空间级别
     */
    private Integer spaceLevel;

    /**
     * 扩容空间大小（GB）
     */
    private Long expandSize;
}
