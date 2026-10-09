package com.sunyujie.picture_backend.model.enums;

import lombok.Getter;

/**
 * 空间图片权限键枚举（RBAC 权限码，JSON 存储的就是这些值）
 */
@Getter
public enum SpacePermissionEnum implements ValueEnum<String> {
    /**
     * 查看空间图片
     */
    PICTURE_VIEW("picture:view"),
    /**
     * 新增空间图片
     */
    PICTURE_ADD("picture:add"),
    /**
     * 修改空间图片
     */
    PICTURE_UPDATE("picture:update"),
    /**
     * 删除空间图片
     */
    PICTURE_DELETE("picture:delete");

    /**
     * 权限码
     */
    private final String value;

    SpacePermissionEnum(String value) {
        this.value = value;
    }
}
