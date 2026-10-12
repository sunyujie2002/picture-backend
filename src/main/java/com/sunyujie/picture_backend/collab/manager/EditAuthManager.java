package com.sunyujie.picture_backend.collab.manager;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.mapper.PictureMapper;
import com.sunyujie.picture_backend.mapper.SpaceMapper;
import com.sunyujie.picture_backend.mapper.SpaceUserMapper;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.model.entity.SpaceUser;
import com.sunyujie.picture_backend.model.enums.SpaceTypeEnum;
import com.sunyujie.picture_backend.model.enums.SpaceRoleEnum;
import com.sunyujie.picture_backend.model.enums.SpacePermissionEnum;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;
import com.sunyujie.picture_backend.collab.model.dto.EditAuthResult;

/**
 * 图片协作编辑权限校验
 * <p>
 * 与图片本身的修改权限保持一致口径（对齐 PictureServiceImpl#updatePicture）：
 * <ul>
 *   <li>公共图（未挂空间）：仅上传人；</li>
 *   <li>个人空间：仅空间创建者；</li>
 *   <li>团队空间：必须拥有 SPACE:{spaceId}:picture:update，即 editor / admin。
 *       <b>viewer 直接拒绝进入编辑器</b>（只读席位不进房间）；</li>
 *   <li>平台管理员对所有图片放行。</li>
 * </ul>
 * 这里没有新增权限模型，完全复用 Sa-Token 的空间 RBAC，避免权限口径分裂。
 */
@Component
public class EditAuthManager {

    /**
     * 图片归属本人（公共图上传人 / 个人空间创建者）时的角色标识，
     * 与团队空间的 viewer/editor/admin 区分开，避免前端混用
     */
    public static final String OWNER_ROLE = "owner";

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private SpaceUserMapper spaceUserMapper;

    /**
     * 校验用户能否进入图片的协作编辑器
     *
     * @param pictureId 图片 id
     * @param userId    用户 id
     * @param userRole  平台角色（user 表的 user_role）
     * @return 鉴权结果，含图片、空间与空间角色信息
     * @throws BusinessException 无权限 / 数据不存在时抛出
     */
    public EditAuthResult authPictureEdit(Long pictureId, Long userId, String userRole) {
        Picture picture = pictureMapper.selectById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        EditAuthResult result = new EditAuthResult();
        result.setPictureId(String.valueOf(pictureId));
        result.setSpaceId(picture.getSpaceId() == null ? null : String.valueOf(picture.getSpaceId()));

        // 平台管理员对所有图片放行
        if (UserRoleEnum.ADMIN.getValue().equals(userRole)) {
            result.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
            result.setCanEdit(true);
            return result;
        }

        // 1. 公共图：仅上传人可编辑
        if (picture.getSpaceId() == null) {
            if (!userId.equals(picture.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有权限编辑该图片");
            }
            result.setSpaceRole(OWNER_ROLE);
            result.setCanEdit(true);
            return result;
        }

        // 2. 空间图
        Space space = spaceMapper.selectById(picture.getSpaceId());
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 3. 个人空间：只认创建者
        if (SpaceTypeEnum.PERSONAL.getValue().equals(space.getSpaceType())) {
            if (!userId.equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有权限编辑该图片");
            }
            result.setSpaceRole(OWNER_ROLE);
            result.setCanEdit(true);
            return result;
        }

        // 4. 团队空间：成员按角色权限判定。
        // 这里刻意不走 StpUtil.hasPermission —— WebSocket 握手并非标准 HTTP 请求，
        // Sa-Token 的 Web 上下文不可用；直接查 space_user + 角色权限表，口径唯一且可测。
        SpaceUser spaceUser = spaceUserMapper.selectOne(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, space.getId())
                .eq(SpaceUser::getUserId, userId));
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该团队空间的成员");
        }
        SpaceRoleEnum roleEnum = SpaceRoleEnum.getByValue(spaceUser.getSpaceRole());
        if (roleEnum == null || !roleEnum.getPermissionValues().contains(SpacePermissionEnum.PICTURE_UPDATE.getValue())) {
            // viewer 角色到此为止：只读成员不允许进入编辑房间
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "浏览者不可编辑图片，仅编辑者和管理员可以");
        }
        result.setSpaceRole(spaceUser.getSpaceRole());
        result.setCanEdit(true);
        return result;
    }
}
