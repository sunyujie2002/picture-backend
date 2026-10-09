package com.sunyujie.picture_backend.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserAddRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserRemoveRequest;
import com.sunyujie.picture_backend.model.dto.spaceUser.SpaceUserUpdateRequest;
import com.sunyujie.picture_backend.model.vo.spaceUser.SpaceUserVO;
import com.sunyujie.picture_backend.service.SpaceUserService;
import com.sunyujie.picture_backend.utils.ResultUtil;

/**
 * 空间成员管理接口
 */
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        return ResultUtil.success(spaceUserService.addSpaceUser(spaceUserAddRequest), "邀请成功");
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody SpaceUserRemoveRequest spaceUserRemoveRequest) {
        return ResultUtil.success(spaceUserService.deleteSpaceUser(spaceUserRemoveRequest), "移除成功");
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateSpaceUser(@RequestBody SpaceUserUpdateRequest spaceUserUpdateRequest) {
        return ResultUtil.success(spaceUserService.updateSpaceUser(spaceUserUpdateRequest), "修改成功");
    }

    @PostMapping("/queryList")
    public BaseResponse<List<SpaceUserVO>> querySpaceUserList(
            @RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        return ResultUtil.success(spaceUserService.querySpaceUserList(spaceUserQueryRequest), "查询成功");
    }
}
