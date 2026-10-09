package com.sunyujie.picture_backend.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.model.dto.space.QuerySpaceRequest;
import com.sunyujie.picture_backend.model.dto.space.SaveSpaceRequest;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.model.vo.space.SpaceVO;

/**
 * Space Service 接口
 */
public interface SpaceService extends IService<Space> {
    /**
     * 新增空间
     * @param space 空间实体
     * @return 新增的实体
     */
    Long addSpace(SaveSpaceRequest spaceRequest);

    /**
     * 更新空间
     * @param spaceRequest 空间实体
     * @return 是否更新成功
     */
    Boolean updateSpace(SaveSpaceRequest spaceRequest);

    /**
     * 删除空间
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    Boolean deleteSpace(DeleteRequest deleteRequest);

    /**
     * 查询空间列表
     * @return 空间列表
     */
    Page<SpaceVO> querySpaceList(QuerySpaceRequest querySpaceRequest);

    /**
     * 根据 id 查询空间
     * @param id 空间 id（为空时按 spaceType 查询当前用户的空间）
     * @param spaceType 空间类型（id 为空时生效，不传默认个人空间）
     * @return 空间
     */
    SpaceVO querySpaceById(Long id, Integer spaceType);

    /**
     * 查询当前登录用户的全部团队空间：我创建的团队空间 + 我被邀请加入的团队空间。
     * 个人空间不返回（仅有一个，走 querySpaceById）。
     *
     * @return 我的团队空间列表（每项回填我在该空间的角色 spaceRole）
     */
    List<SpaceVO> queryMySpaceList();
}
