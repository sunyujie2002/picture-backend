package com.sunyujie.picture_backend.model.dto.picture;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import lombok.Data;

/**
 * 批量更新图片请求：仅允许更新分类和标签
 */
@Data
public class BatchUpdatePictureRequest {

    /**
     * 图片 id 列表
     */
    @NotEmpty(message = "图片id列表不能为空")
    private List<Long> pictureIds;

    /**
     * 分类 id（不传表示不更新分类）
     */
    private Long categoryId;

    /**
     * 标签 id 列表（不传表示不更新标签；传空列表表示清空标签）
     */
    private List<Long> tagIds;
}
