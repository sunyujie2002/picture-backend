package com.sunyujie.picture_backend.model.dto.tag;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 更新标签请求（仅管理员）：可更新标签名称与排序值
 */
@Data
public class UpdateTagRequest {

    /**
     * id
     */
    @NotNull(message = "标签ID不能为空")
    private Long id;

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    /**
     * 排序值（越小越靠前）；不传表示不改动排序
     */
    private Integer sort;
}
