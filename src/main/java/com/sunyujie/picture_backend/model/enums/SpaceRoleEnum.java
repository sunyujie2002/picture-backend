package com.sunyujie.picture_backend.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

/**
 * 空间角色枚举：
 * viewer 只读；editor 可增删改（不能删？editor 可编辑）；admin 空间管理员
 */
@Getter
public enum SpaceRoleEnum implements ValueEnum<String> {
    /**
     * 浏览者：只读
     */
    VIEWER("viewer", "浏览者", new SpacePermissionEnum[] {
            SpacePermissionEnum.PICTURE_VIEW
    }),
    /**
     * 编辑者：查看 + 新增 + 修改
     */
    EDITOR("editor", "编辑者", new SpacePermissionEnum[] {
            SpacePermissionEnum.PICTURE_VIEW,
            SpacePermissionEnum.PICTURE_ADD,
            SpacePermissionEnum.PICTURE_UPDATE
    }),
    /**
     * 空间管理员：全部权限
     */
    ADMIN("admin", "管理员", new SpacePermissionEnum[] {
            SpacePermissionEnum.PICTURE_VIEW,
            SpacePermissionEnum.PICTURE_ADD,
            SpacePermissionEnum.PICTURE_UPDATE,
            SpacePermissionEnum.PICTURE_DELETE
    });

    /**
     * 角色值（存 space_user.space_role）
     */
    private final String value;

    private final String label;

    /**
     * 该角色拥有的权限集合（写入 permissions JSON）
     */
    private final List<SpacePermissionEnum> permissions;

    SpaceRoleEnum(String value, String label, SpacePermissionEnum[] permissions) {
        this.value = value;
        this.label = label;
        this.permissions = Arrays.asList(permissions);
    }

    /**
     * 获取角色的权限码列表
     */
    public List<String> getPermissionValues() {
        return permissions.stream()
                .map(SpacePermissionEnum::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 按角色值反查枚举（viewer/editor/admin），非法值返回 null，不抛异常
     */
    public static SpaceRoleEnum getByValue(String value) {
        return Arrays.stream(values())
                .filter(roleEnum -> roleEnum.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
