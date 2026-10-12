package com.sunyujie.picture_backend.model.vo.analysis;

import lombok.Data;

/**
 * 某个统计周期内单个用户的上传量
 *
 * 只带用户名不带 userId：前端不需要（也不应该）展示裸 ID
 */
@Data
public class UserUploadStatVO {
    /**
     * 用户名
     */
    private String userName;

    /**
     * 该用户在该周期内上传的图片数量
     */
    private Long pictureCount;
}
