package com.sunyujie.picture_backend.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserAddRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserRemoveRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserUpdateRequest;
import com.sunyujie.picture_backend.model.entity.SpaceUser;
import com.sunyujie.picture_backend.model.vo.spaceUser.SpaceUserVO;

/**
 * 空间用户关联 Service
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 邀请成员加入空间（角色仅 viewer / editor）
     *
     * @return 关联记录 id
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 移除空间成员（admin 不允许移除）
     */
    Boolean deleteSpaceUser(SpaceUserRemoveRequest spaceUserRemoveRequest);

    /**
     * 修改空间成员角色（仅 viewer / editor 之间互改，空间创建者 admin 不允许修改）
     */
    Boolean updateSpaceUser(SpaceUserUpdateRequest spaceUserUpdateRequest);

    /**
     * 查询空间成员列表
     */
    List<SpaceUserVO> querySpaceUserList(SpaceUserQueryRequest spaceUserQueryRequest);
}
