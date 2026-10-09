package com.sunyujie.picture_backend.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.manager.SpaceAuthManager;
import com.sunyujie.picture_backend.manager.async.AsyncCacheManager;
import com.sunyujie.picture_backend.mapper.PictureMapper;
import com.sunyujie.picture_backend.mapper.SpaceMapper;
import com.sunyujie.picture_backend.mapper.SpaceUserMapper;
import com.sunyujie.picture_backend.mapper.UserMapper;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserAddRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserRemoveRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserUpdateRequest;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.model.entity.SpaceUser;
import com.sunyujie.picture_backend.model.entity.User;
import com.sunyujie.picture_backend.model.enums.SpacePermissionEnum;
import com.sunyujie.picture_backend.model.enums.SpaceRoleEnum;
import com.sunyujie.picture_backend.model.enums.SpaceTypeEnum;
import com.sunyujie.picture_backend.model.vo.spaceUser.SpaceUserVO;
import com.sunyujie.picture_backend.service.SpaceUserService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 空间用户关联 Service 实现
 * <p>
 * 成员管理仅团队空间可用；admin 只能随空间创建产生，不能邀请、不能被移除。
 * 角色权限由 SpaceRoleEnum 推导，数据库只存角色。
 */
@Slf4j
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SpaceAuthManager spaceAuthManager;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private AsyncCacheManager asyncCacheManager;

    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        Long spaceId = spaceUserAddRequest.getSpaceId();
        Long targetUserId = spaceUserAddRequest.getUserId();
        String roleValue = spaceUserAddRequest.getSpaceRole();
        if (ObjectUtil.hasEmpty(spaceId, targetUserId) || StrUtil.isBlank(roleValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间、用户或角色不能为空");
        }
        // 空间必须存在；成员管理是团队空间能力，个人空间直接拒绝
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        if (!SpaceTypeEnum.TEAM.getValue().equals(space.getSpaceType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人空间不支持邀请成员");
        }
        // 仅空间 admin（或平台管理员）可邀请成员
        spaceAuthManager.checkAdmin(spaceId);
        // 被邀请用户必须存在
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "被邀请的用户不存在");
        }
        // 邀请角色仅支持 viewer / editor：admin 只能随空间创建产生
        SpaceRoleEnum roleEnum = SpaceRoleEnum.getByValue(roleValue.trim());
        if (roleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不存在，仅支持 viewer / editor");
        }
        if (roleEnum == SpaceRoleEnum.ADMIN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能邀请管理员");
        }
        // 只查活跃成员（@TableLogic 自动追加 delete_time IS NULL）：在邀直接拒绝；
        // 曾被移除的用户查不到，直接新增一条关联记录，不复用历史记录
        SpaceUser existing = baseMapper.selectOne(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, targetUserId));
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已是空间成员");
        }
        LocalDateTime now = LocalDateTime.now();
        Long spaceUserId;
        try {
            SpaceUser spaceUser = new SpaceUser();
            spaceUser.setSpaceId(spaceId);
            spaceUser.setUserId(targetUserId);
            spaceUser.setSpaceRole(roleEnum.getValue());
            spaceUser.setCreateTime(now);
            baseMapper.insert(spaceUser);
            spaceUserId = spaceUser.getId();
        } catch (Exception e) {
            log.error("add space user error, request = {}", spaceUserAddRequest, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邀请成员失败");
        }
        // 被邀请用户权限已变化，清其缓存（未登录则无缓存可清）
        spaceAuthManager.evictAuthCache(targetUserId);
        return spaceUserId;
    }

    @Override
    public Boolean deleteSpaceUser(SpaceUserRemoveRequest spaceUserRemoveRequest) {
        Long spaceId = spaceUserRemoveRequest.getSpaceId();
        Long targetUserId = spaceUserRemoveRequest.getUserId();
        if (ObjectUtil.hasEmpty(spaceId, targetUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间或用户不能为空");
        }
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        if (!SpaceTypeEnum.TEAM.getValue().equals(space.getSpaceType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人空间不支持移除成员");
        }
        // 仅空间 admin（或平台管理员）可移除成员
        spaceAuthManager.checkAdmin(spaceId);
        SpaceUser spaceUser = baseMapper.selectOne(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, targetUserId));
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该用户不是空间成员");
        }
        // admin（空间创建者）不允许通过成员接口移除，只能随空间一起删除
        if (SpaceRoleEnum.ADMIN.getValue().equals(spaceUser.getSpaceRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间管理员不能被移除");
        }
        try {
            // 逻辑删除：delete_time 置为当前时间
            baseMapper.deleteById(spaceUser.getId());
        } catch (Exception e) {
            log.error("delete space user error, request = {}", spaceUserRemoveRequest, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "移除成员失败");
        }
        // 被移除用户权限已变化，清其账户会话中的权限缓存
        spaceAuthManager.evictAuthCache(targetUserId);
        // 该空间全部图片的详情缓存异步清理：避免被移除用户在缓存 TTL 内凭旧 id 仍读到图片
        List<Long> pictureIds = pictureMapper.selectList(new LambdaQueryWrapper<Picture>()
                .select(Picture::getId)
                .eq(Picture::getSpaceId, spaceId))
                .stream()
                .map(Picture::getId)
                .collect(Collectors.toList());
        asyncCacheManager.evictPictureCache(pictureIds);
        return true;
    }

    @Override
    public Boolean updateSpaceUser(SpaceUserUpdateRequest spaceUserUpdateRequest) {
        Long spaceId = spaceUserUpdateRequest.getSpaceId();
        Long targetUserId = spaceUserUpdateRequest.getUserId();
        String roleValue = spaceUserUpdateRequest.getSpaceRole();
        if (ObjectUtil.hasEmpty(spaceId, targetUserId) || StrUtil.isBlank(roleValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间、用户或角色不能为空");
        }
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        if (!SpaceTypeEnum.TEAM.getValue().equals(space.getSpaceType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人空间不支持修改成员角色");
        }
        // 仅空间 admin（或平台管理员）可修改成员角色
        spaceAuthManager.checkAdmin(spaceId);
        SpaceUser spaceUser = baseMapper.selectOne(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, targetUserId));
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该用户不是空间成员");
        }
        // 空间创建者 admin 的角色不允许修改，只能随空间一起删除
        if (SpaceRoleEnum.ADMIN.getValue().equals(spaceUser.getSpaceRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间管理员角色不能被修改");
        }
        // 目标角色仅支持 viewer / editor
        SpaceRoleEnum targetRoleEnum = SpaceRoleEnum.getByValue(roleValue.trim());
        if (targetRoleEnum == null || targetRoleEnum == SpaceRoleEnum.ADMIN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不存在，仅支持 viewer / editor");
        }
        // 角色未变化：无需更新、无需清缓存，直接返回成功
        if (targetRoleEnum.getValue().equals(spaceUser.getSpaceRole())) {
            return true;
        }
        try {
            SpaceUser updateEntity = new SpaceUser();
            updateEntity.setId(spaceUser.getId());
            updateEntity.setSpaceRole(targetRoleEnum.getValue());
            baseMapper.updateById(updateEntity);
        } catch (Exception e) {
            log.error("update space user role error, request = {}", spaceUserUpdateRequest, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改成员角色失败");
        }
        // 目标用户权限已变化，清其缓存（无需清图片缓存：图片未变化，仅权限变化）
        spaceAuthManager.evictAuthCache(targetUserId);
        return true;
    }

    @Override
    public List<SpaceUserVO> querySpaceUserList(SpaceUserQueryRequest spaceUserQueryRequest) {
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        if (spaceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间ID不能为空");
        }
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 个人空间仅创建者可查（结果为空）；团队空间成员可查看成员列表，非成员拒绝
        spaceAuthManager.checkSpacePermission(space, SpacePermissionEnum.PICTURE_VIEW);
        List<SpaceUser> spaceUserList = baseMapper.selectList(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, spaceId)
                .orderByAsc(SpaceUser::getCreateTime));
        if (spaceUserList.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查用户信息（只取简要信息所需列），避免逐个查询 N+1
        List<Long> userIds = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUserAccount, User::getUserName, User::getUserAvatar)
                .in(User::getId, userIds))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return spaceUserList.stream().map(spaceUser -> {
            SpaceUserVO vo = BeanUtil.copyProperties(spaceUser, SpaceUserVO.class);
            User user = userMap.get(spaceUser.getUserId());
            if (user == null) {
                vo.setUserName("用户已注销");
            } else {
                vo.setUserAccount(user.getUserAccount());
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
            // 权限由角色推导返回给前端，数据库不存权限码
            SpaceRoleEnum roleEnum = SpaceRoleEnum.getByValue(spaceUser.getSpaceRole());
            vo.setPermissions(roleEnum == null ? new ArrayList<>() : roleEnum.getPermissionValues());
            return vo;
        }).collect(Collectors.toList());
    }
}
