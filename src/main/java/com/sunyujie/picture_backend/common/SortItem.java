package com.sunyujie.picture_backend.common;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 排序项：分类 / 标签拖拽排序后的「id -> 排序值」对
 */
@Data
public class SortItem {

    /**
     * 分类 / 标签 id
     */
    @NotNull(message = "ID不能为空")
    private Long id;

    /**
     * 排序值（越小越靠前）
     */
    @NotNull(message = "排序值不能为空")
    private Integer sort;
}
