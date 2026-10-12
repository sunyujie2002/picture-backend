package com.sunyujie.picture_backend.model.vo.category;

import lombok.Data;

/**
 * 分类 VO（面向普通用户：仅暴露必要字段）
 */
@Data
public class CategoryVO {

    /**
     * id
     */
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;
}
