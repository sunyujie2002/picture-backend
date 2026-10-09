package com.sunyujie.picture_backend.service.impl;

import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.mapper.PictureMapper;
import com.sunyujie.picture_backend.mapper.SpaceMapper;
import com.sunyujie.picture_backend.mapper.SpaceUserMapper;
import com.sunyujie.picture_backend.model.dto.space.QuerySpaceRequest;
import com.sunyujie.picture_backend.model.dto.space.SaveSpaceRequest;
import com.sunyujie.picture_backend.model.dto.user.UserDTO;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.model.entity.SpaceUser;
import com.sunyujie.picture_backend.model.enums.SpaceLevelEnum;
import com.sunyujie.picture_backend.model.enums.SpaceRoleEnum;
import com.sunyujie.picture_backend.model.enums.SpaceTypeEnum;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;
import com.sunyujie.picture_backend.manager.SpaceAuthManager;
import com.sunyujie.picture_backend.manager.async.AsyncCacheManager;
import com.sunyujie.picture_backend.model.vo.space.SpaceVO;
import com.sunyujie.picture_backend.service.SpaceService;
import com.sunyujie.picture_backend.service.UserService;
import com.sunyujie.picture_backend.utils.EnumUtil;
import com.sunyujie.picture_backend.utils.QueryWrapperUtil;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Space Service 实现类
 */
@Slf4j
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    /**
     * 创建空间的分布式内锁：必须类级别共享，跨请求才生效；
     * key 为 userId，防止同一用户并发重复创建
     */
    private static final Map<Long, Object> LOCK_MAP = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private SpaceUserMapper spaceUserMapper;

    @Resource
    private SpaceAuthManager spaceAuthManager;

    @Resource
    private AsyncCacheManager asyncCacheManager;

    @Override
    public Long addSpace(SaveSpaceRequest spaceRequest) {
        ThrowUtil.throwIf(ObjectUtil.isEmpty(spaceRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtil.throwIf(spaceRequest.getSpaceId() != null,
                ErrorCode.PARAMS_ERROR,
                "新增空间时,不能指定空间ID");
        // 空间类型：不传默认个人空间，兼容旧前端
        SpaceTypeEnum spaceTypeEnum = spaceRequest.getSpaceType() == null
                ? SpaceTypeEnum.PERSONAL
                : EnumUtil.getEnumByValue(spaceRequest.getSpaceType(), SpaceTypeEnum.class);
        ThrowUtil.throwIf(spaceTypeEnum == null, ErrorCode.PARAMS_ERROR, "参数错误,空间类型不存在");
        String spaceName = spaceRequest.getSpaceName();
        if (StrUtil.isBlank(spaceName)) {
            String username = StrUtil.isNotBlank(UserContext.getLoginUser().getUserName())
                    ? UserContext.getLoginUser().getUserName()
                    : "默认用户";
            spaceName = String.format("%s的%s", username, spaceTypeEnum.getLabel());
        }
        Space space = new Space();
        space.setSpaceName(spaceName);
        space.setSpaceType(spaceTypeEnum.getValue());
        space.setSpaceLevel(0);
        space.setUserId(UserContext.getLoginUserId());
        fillSpaceByLevel(space);
        Long userId = UserContext.getLoginUserId();
        Object lock = LOCK_MAP.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            try {
                // 同一用户下，每种空间类型仅能创建一个
                Boolean existingSpace = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, spaceTypeEnum.getValue())
                        .exists();
                ThrowUtil.throwIf(existingSpace, ErrorCode.PARAMS_ERROR,
                        String.format("每个用户仅能创建一个%s", spaceTypeEnum.getLabel()));
                if (spaceTypeEnum == SpaceTypeEnum.TEAM) {
                    // 团队空间：空间 + 创建者 admin 关联在同一事务内写入
                    transactionTemplate.executeWithoutResult(status -> {
                        baseMapper.insert(space);
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        spaceUser.setCreateTime(LocalDateTime.now());
                        spaceUserMapper.insert(spaceUser);
                    });
                } else {
                    // 个人空间：不写关联记录，图片权限只认空间创建者，与原逻辑保持一致
                    baseMapper.insert(space);
                }
                return space.getId();
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增空间失败");
            } finally {
                // 防止内存泄漏
                LOCK_MAP.remove(userId);
            }
        }
    }

    @Override
    public Boolean updateSpace(SaveSpaceRequest spaceRequest) {
        ThrowUtil.throwIf(ObjectUtil.isEmpty(spaceRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtil.throwIf(spaceRequest.getSpaceId() == null,
                ErrorCode.PARAMS_ERROR,
                "更新空间时,必须指定空间ID");
        // 老数据
        Space oldSpace = checkSpaceAuth(spaceRequest.getSpaceId());
        // 只有管理员可以修改空间等级和扩展大小
        ThrowUtil.throwIf(!ObjectUtil.isAllEmpty(spaceRequest.getSpaceLevel(), spaceRequest.getExpandSize()) &&
                !UserContext.getRole().equals(UserRoleEnum.ADMIN.getValue()),
                ErrorCode.PARAMS_ERROR,
                "您没有权限更新该空间");
        // 新数据
        Space space = BeanUtil.copyProperties(spaceRequest, Space.class);
        if (ObjectUtil.isNotEmpty(space.getSpaceLevel())) {
            fillSpaceByLevel(space);
        }
        // 旧的扩展大小
        Long oldExpandSize = oldSpace.getMaxSize()
                - EnumUtil.getEnumByValue(oldSpace.getSpaceLevel(), SpaceLevelEnum.class).getSize();
        Long expandSize = spaceRequest.getExpandSize() * 1024 * 1024;
        Long newMaxSize = space.getMaxSize() + expandSize + oldExpandSize;
        Long newMaxCount = newMaxSize * SpaceLevelEnum.COUNT_UNIT;
        space.setMaxSize(newMaxSize);
        space.setMaxCount(newMaxCount);
        space.setEditTime(LocalDateTime.now());
        try {
            baseMapper.updateById(space);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新空间失败");
        }
        return true;
    }

    @Override
    @AuthCheck
    public Boolean deleteSpace(DeleteRequest deleteRequest) {
        ThrowUtil.throwIf(ObjectUtil.isEmpty(deleteRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtil.throwIf(deleteRequest.getId() == null,
                ErrorCode.PARAMS_ERROR,
                "删除空间时,必须指定空间ID");
        checkSpaceAuth(deleteRequest.getId());
        // 删除前先查出成员列表，事务后用于清各成员的权限缓存
        List<SpaceUser> memberList = spaceUserMapper.selectList(
                new LambdaQueryWrapper<SpaceUser>().eq(SpaceUser::getSpaceId, deleteRequest.getId()));
        // 删除前先查出空间内全部图片 id：事务删图后 @TableLogic 会过滤，事后查不到，
        // 事务后用这批 id 异步清图片详情缓存
        List<Long> pictureIds = pictureMapper.selectList(new LambdaQueryWrapper<Picture>()
                        .select(Picture::getId)
                        .eq(Picture::getSpaceId, deleteRequest.getId()))
                .stream()
                .map(Picture::getId)
                .collect(Collectors.toList());
        transactionTemplate.executeWithoutResult(status -> {
            try {
                baseMapper.deleteById(deleteRequest.getId());
                // 删除空间内图片：此处只需基础删除，直接用 Mapper，避免注入 PictureService 形成循环依赖
                pictureMapper.delete(new LambdaQueryWrapper<Picture>().eq(Picture::getSpaceId, deleteRequest.getId()));
                // 删除空间用户关联
                spaceUserMapper.delete(new LambdaQueryWrapper<SpaceUser>().eq(SpaceUser::getSpaceId,
                        deleteRequest.getId()));
            } catch (Exception e) {
                log.error("delete space error, spaceId = {}", deleteRequest.getId(), e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除空间失败");
            }
        });
        // 事务成功后清各成员缓存（缓存里残留的空间权限码不再有对应数据）
        memberList.stream()
                .map(SpaceUser::getUserId)
                .distinct()
                .forEach(spaceAuthManager::evictAuthCache);
        // 异步清理空间内图片的详情两级缓存，避免缓存 TTL 内仍能读到已删除的图片
        asyncCacheManager.evictPictureCache(pictureIds);
        return true;
    }

    private void fillSpaceByLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = EnumUtil.getEnumByValue(space.getSpaceLevel(), SpaceLevelEnum.class);
        ThrowUtil.throwIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "参数错误,空间等级不存在");
        space.setSpaceLevel(spaceLevelEnum.getValue());
        space.setMaxSize(spaceLevelEnum.getSize());
        space.setMaxCount(spaceLevelEnum.getCount());
    }

    @Override
    @AuthCheck
    public Page<SpaceVO> querySpaceList(QuerySpaceRequest querySpaceRequest) {
        ThrowUtil.throwIf(ObjectUtil.isEmpty(querySpaceRequest), ErrorCode.PARAMS_ERROR);
        Page<Space> page = new Page<>(querySpaceRequest.getPageNum(), querySpaceRequest.getPageSize());
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        Integer spaceType = querySpaceRequest.getSpaceType();
        Integer spaceLevel = querySpaceRequest.getSpaceLevel();
        String spaceName = querySpaceRequest.getSpaceName();
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceType), "space_type", spaceType);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceLevel), "space_level", spaceLevel);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "space_name", spaceName);
        QueryWrapperUtil.buildSortQueryWrapper(queryWrapper, querySpaceRequest.getSortField(),
                querySpaceRequest.getSortOrder());
        baseMapper.selectPage(page, queryWrapper);
        Page<SpaceVO> spaceVOPage = new Page<>(page.getCurrent(), page.getSize());
        List<Long> userIds = page.getRecords().stream().map(Space::getUserId).collect(Collectors.toList());
        Map<Long, String> userMap = userService.getUserNameMap(userIds);
        List<SpaceVO> spaceVOList = page.getRecords().stream().map(space -> {
            SpaceVO spaceVO = BeanUtil.copyProperties(space, SpaceVO.class);
            spaceVO.setUserName(userMap.getOrDefault(space.getUserId(), "用户已注销"));
            return spaceVO;
        }).collect(Collectors.toList());
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public SpaceVO querySpaceById(Long id, Integer spaceType) {
        Space space;
        // 如果 id 为空，则按类型查询当前登录用户的空间（不传类型默认个人空间）
        if (ObjectUtil.isEmpty(id)) {
            SpaceTypeEnum spaceTypeEnum = spaceType == null
                    ? SpaceTypeEnum.PERSONAL
                    : EnumUtil.getEnumByValue(spaceType, SpaceTypeEnum.class);
            ThrowUtil.throwIf(spaceTypeEnum == null, ErrorCode.PARAMS_ERROR, "参数错误,空间类型不存在");
            Long userId = UserContext.getLoginUserId();
            space = baseMapper.selectOne(new LambdaQueryWrapper<Space>()
                    .eq(Space::getUserId, userId)
                    .eq(Space::getSpaceType, spaceTypeEnum.getValue()));
            // 没创建空间是正常的初始状态（如刷新页面），不报错，返回 null 由前端引导创建
            if (space == null) {
                return null;
            }
        } else {
            space = checkSpaceAuth(id);
        }
        SpaceVO spaceVO = BeanUtil.copyProperties(space, SpaceVO.class);
        List<Long> userIds = Arrays.asList(spaceVO.getUserId());
        Map<Long, String> userMap = userService.getUserNameMap(userIds);
        spaceVO.setUserName(userMap.getOrDefault(spaceVO.getUserId(), "用户已注销"));
        return spaceVO;
    }

    @Override
    public List<SpaceVO> queryMySpaceList() {
        Long userId = UserContext.getLoginUserId();
        // ① 我创建的团队空间（每人至多一个），我在其中的角色为 admin
        List<Space> ownedTeamSpaceList = this.lambdaQuery()
                .eq(Space::getUserId, userId)
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue())
                .list();
        Set<Long> ownedSpaceIds = ownedTeamSpaceList.stream()
                .map(Space::getId)
                .collect(Collectors.toSet());
        // ② 我加入的全部空间关联（创建团队时也会给我写一条 admin 关联，所以第③步要按 id 去重）
        Map<Long, String> joinedRoleMap = spaceUserMapper.selectList(new LambdaQueryWrapper<SpaceUser>()
                        .eq(SpaceUser::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(SpaceUser::getSpaceId, SpaceUser::getSpaceRole,
                        (oldValue, newValue) -> oldValue));
        List<Long> joinedSpaceIds = joinedRoleMap.keySet().stream()
                .filter(spaceId -> !ownedSpaceIds.contains(spaceId))
                .collect(Collectors.toList());
        List<Space> joinedSpaceList = joinedSpaceIds.isEmpty()
                ? new ArrayList<>()
                : baseMapper.selectBatchIds(joinedSpaceIds);
        // ③ 组装 VO（个人空间不返回，单个空间走 queryById）
        List<Space> allSpaceList = new ArrayList<>(ownedTeamSpaceList.size() + joinedSpaceList.size());
        allSpaceList.addAll(ownedTeamSpaceList);
        allSpaceList.addAll(joinedSpaceList);
        Map<Long, String> creatorNameMap = userService.getUserNameMap(
                allSpaceList.stream().map(Space::getUserId).collect(Collectors.toList()));
        List<SpaceVO> spaceVOList = allSpaceList.stream().map(space -> {
            SpaceVO spaceVO = BeanUtil.copyProperties(space, SpaceVO.class);
            spaceVO.setUserName(creatorNameMap.getOrDefault(space.getUserId(), "用户已注销"));
            // 我创建的 → admin；我加入的 → 取关联表角色
            spaceVO.setSpaceRole(ownedSpaceIds.contains(space.getId())
                    ? SpaceRoleEnum.ADMIN.getValue()
                    : joinedRoleMap.get(space.getId()));
            return spaceVO;
        }).collect(Collectors.toList());
        // 全部为团队空间，按创建时间升序即可
        spaceVOList.sort(Comparator.comparing(SpaceVO::getCreateTime));
        return spaceVOList;
    }

    /**
     * 在已经有空间ID情况下，检查空间是否存在且当前登录用户是否有权限操作该空间。
     * <p>
     * 当前个人空间和团队空间都只有创建者可访问（管理员除外）；
     * 后续团队空间增加成员体系后，在此处补充"团队空间成员也放行"的判断。
     *
     * @param spaceId 空间ID
     * @return 空间实体
     */
    private Space checkSpaceAuth(Long spaceId) {
        Space space = baseMapper.selectById(spaceId);
        // 按客户端指定的具体 id 定位资源：不存在是 404，不是"空数据"（区别于不带 id 查"我的空间"）
        ThrowUtil.throwIf(ObjectUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        UserDTO user = UserContext.getLoginUser();
        // 常量放前面，避免 userRole 为 null 时 NPE
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
        boolean isOwner = ObjectUtil.equals(space.getUserId(), user.getId());
        ThrowUtil.throwIf(!isAdmin && !isOwner, ErrorCode.NO_AUTH_ERROR, "您没有权限访问该空间");
        return space;
    }
}
