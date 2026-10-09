package com.sunyujie.picture_backend.model.dto.picture;

import lombok.Data;

/**
 * 审核图片请求
 */
@Data
public class ReviewPictureRequest {
    /**
     * 图片id
     */
    private Long pictureId;
    /**
     * 审核状态
     */
    private Integer reviewStatus;
    /**
     * 审核消息
     */
    private String reviewMessage;
}
