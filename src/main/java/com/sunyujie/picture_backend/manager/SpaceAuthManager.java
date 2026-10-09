package com.sunyujie.picture_backend.manager;

import org.springframework.stereotype.Component;

import com.sunyujie.picture_backend.constant.SaTokenConstant;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.model.enums.SpacePermissionEnum;
import com.sunyujie.picture_backend.model.enums.SpaceTypeEnum;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;

/**
 * 空间权限校验管理器：业务代码统一从这里做空间级鉴权
 * <p>
 * 个人空间（space_type=0）：只认空间创建者，与 RBAC 无关，保持原有逻辑；
 * 团队空间（space_type=1）：走 Sa-Token 空间 RBAC，
 * 权限码带空间维度 SPACE:{spaceId}:picture:xxx；
 * 系统管理员（平台 admin）直接放行，保持"管理员可管理任意空间"的行为。
 */
@Component
public class SpaceAuthManager {

    /**
     * 图片操作权限校验，不通过抛 NotPermissionException：
     * 个人空间要求是空间创建者；团队空间要求拥有对应空间权限码
     */
    public void checkSpacePermission(Space space, SpacePermissionEnum permission) {
        if (isPlatformAdmin()) {
            return;
        }
        if (SpaceTypeEnum.PERSONAL.getValue().equals(space.getSpaceType())) {
            boolean isOwner = space.getUserId().equals(UserContext.getLoginUserId());
            if (!isOwner) {
                throw new NotPermissionException(permission.getValue(), StpUtil.stpLogic.getLoginType());
            }
            return;
        }
        StpUtil.checkPermission(encodeScope(space.getId()) + permission.getValue());
    }

    /**
     * 校验当前用户是否是指定空间的 admin，不通过抛 NotRoleException。
     * 仅团队空间存在成员管理，调用方应先拦截个人空间。
     */
    public void checkAdmin(Long spaceId) {
        if (isPlatformAdmin()) {
            return;
        }
        StpUtil.checkRole(encodeScope(spaceId) + "admin");
    }

    /**
     * 成员角色 / 权限变更后，清除该用户的权限、角色缓存（下次校验自动重建）
     */
    public void evictAuthCache(Long userId) {
        SaSession session = StpUtil.getSessionByLoginId(userId, false);
        if (session == null) {
            return;
        }
        session.delete(SaTokenConstant.PERMISSION_LIST);
        session.delete(SaTokenConstant.ROLE_LIST);
    }

    private String encodeScope(Long spaceId) {
        return SaTokenConstant.SPACE_SCOPE_PREFIX + spaceId + ":";
    }

    /**
     * 平台系统管理员（user 表的 admin 角色）
     */
    private boolean isPlatformAdmin() {
        return UserRoleEnum.ADMIN.getValue().equals(UserContext.getRole());
    }
}
