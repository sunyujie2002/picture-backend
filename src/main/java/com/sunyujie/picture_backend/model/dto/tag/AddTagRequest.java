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
}
