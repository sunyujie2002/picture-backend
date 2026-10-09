package com.sunyujie.picture_backend.model.dto.category;

import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * 新增分类请求（仅管理员）
 */
@Data
public class AddCategoryRequest {

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
}
