package com.sunyujie.picture_backend.model.dto.picture;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import lombok.Data;

/**
 * 批量删除图片请求
 */
@Data
public class BatchDeletePictureRequest {

    /**
     * 图片 id 列表
     */
    @NotEmpty(message = "图片id列表不能为空")
    private List<Long> pictureIds;
}
