package com.sunyujie.picture_backend.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sunyujie.picture_backend.constant.SaTokenConstant;
import com.sunyujie.picture_backend.mapper.SpaceUserMapper;
import com.sunyujie.picture_backend.model.entity.SpaceUser;
import com.sunyujie.picture_backend.model.enums.SpaceRoleEnum;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Sa-Token 权限 / 角色数据源实现
 * <p>
 * sa-token 每次 @SaCheckPermission / checkPermission 时回调本类；
 * 数据库只存角色（space_role），权限由 SpaceRoleEnum 角色映射推导，不做冗余存储；
 * 权限码带空间维度（SPACE:{spaceId}:xxx），结果缓存进账户会话，
 * 成员角色变更后由 SpaceAuthManager 主动清缓存。
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserMapper spaceUserMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SaSession session = StpUtil.getSessionByLoginId(userId);
        @SuppressWarnings("unchecked")
        List<String> permissionList = (List<String>) session.get(SaTokenConstant.PERMISSION_LIST);
        if (permissionList == null) {
            permissionList = loadPermissions(userId);
            session.set(SaTokenConstant.PERMISSION_LIST, permissionList);
        }
        return permissionList;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SaSession session = StpUtil.getSessionByLoginId(userId);
        @SuppressWarnings("unchecked")
        List<String> roleList = (List<String>) session.get(SaTokenConstant.ROLE_LIST);
        if (roleList == null) {
            roleList = loadRoles(userId);
            session.set(SaTokenConstant.ROLE_LIST, roleList);
        }
        return roleList;
    }

    /**
     * 查询用户全部空间关联，按角色映射权限，编码为 SPACE:{spaceId}:{permission}
     */
    private List<String> loadPermissions(Long userId) {
        List<SpaceUser> spaceUserList = spaceUserMapper.selectList(
                new LambdaQueryWrapper<SpaceUser>().eq(SpaceUser::getUserId, userId));
        List<String> permissionList = new ArrayList<>();
        for (SpaceUser spaceUser : spaceUserList) {
            SpaceRoleEnum roleEnum = SpaceRoleEnum.getByValue(spaceUser.getSpaceRole());
            if (roleEnum == null) {
                log.warn("unknown space role skipped, spaceId = {}, userId = {}, role = {}",
                        spaceUser.getSpaceId(), userId, spaceUser.getSpaceRole());
                continue;
            }
            for (String permission : roleEnum.getPermissionValues()) {
                permissionList.add(SaTokenConstant.SPACE_SCOPE_PREFIX + spaceUser.getSpaceId() + ":" + permission);
            }
        }
        return permissionList;
    }

    /**
     * 查询用户的空间角色标识：SPACE:{spaceId}:{spaceRole}
     */
    private List<String> loadRoles(Long userId) {
        List<SpaceUser> spaceUserList = spaceUserMapper.selectList(
                new LambdaQueryWrapper<SpaceUser>().eq(SpaceUser::getUserId, userId));
        List<String> roleList = new ArrayList<>();
        for (SpaceUser spaceUser : spaceUserList) {
            roleList.add(SaTokenConstant.SPACE_SCOPE_PREFIX + spaceUser.getSpaceId() + ":" + spaceUser.getSpaceRole());
        }
        return roleList;
    }
}
