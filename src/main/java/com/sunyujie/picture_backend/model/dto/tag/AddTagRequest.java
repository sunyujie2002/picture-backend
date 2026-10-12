package com.sunyujie.picture_backend.model.dto.tag;

import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * 新增标签请求（仅管理员）
 */
@Data
public class AddTagRequest {

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    /**
     * 排序值（越小越靠前）；不传则自动排在最后
     */
    private Integer sort;
}
