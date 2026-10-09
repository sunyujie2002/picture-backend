package com.sunyujie.picture_backend.constant;

/**
 * Sa-Token 会话属性 key 常量
 */
public interface SaTokenConstant {
    /**
     * 账户会话中缓存的脱敏登录用户信息（UserDTO）
     */
    String LOGIN_USER = "loginUser";

    /**
     * 账户会话中缓存的权限码列表
     */
    String PERMISSION_LIST = "permissionList";

    /**
     * 账户会话中缓存的空间角色标识列表
     */
    String ROLE_LIST = "roleList";

    /**
     * 空间权限 / 角色编码前缀：
     * 权限形如 SPACE:123:picture:view；角色形如 SPACE:123:admin
     * （权限是"空间级别"的，同一用户在不同空间权限不同，必须把 spaceId 编进码里）
     */
    String SPACE_SCOPE_PREFIX = "SPACE:";
}
