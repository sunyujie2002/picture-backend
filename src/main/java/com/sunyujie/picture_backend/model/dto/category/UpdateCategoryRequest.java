package com.sunyujie.picture_backend.model.dto.category;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 更新分类请求（仅管理员）
 */
@Data
public class UpdateCategoryRequest {

    /**
     * id
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
}
