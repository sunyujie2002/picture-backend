package com.sunyujie.picture_backend.collab.model.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * 图片协作鉴权结果
 */
@Data
public class EditAuthResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片 id（字符串）
     */
    private String pictureId;

    /**
     * 所属空间 id，公共图为 null
     */
    private String spaceId;

    /**
     * 角色标识：owner / viewer / editor / admin
     */
    private String spaceRole;

    /**
     * 是否允许编辑（viewer 或越权时 false）
     */
    private Boolean canEdit;
}
