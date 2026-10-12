package com.sunyujie.picture_backend.model.vo.tag;

import lombok.Data;

/**
 * 标签 VO（面向普通用户：仅暴露必要字段）
 */
@Data
public class TagVO {

    /**
     * id
     */
    private Long id;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 排序值（越小越靠前）
     */
    private Integer sort;
}
