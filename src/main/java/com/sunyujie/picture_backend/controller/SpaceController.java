package com.sunyujie.picture_backend.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.model.dto.space.QuerySpaceRequest;
import com.sunyujie.picture_backend.model.dto.space.SaveSpaceRequest;
import com.sunyujie.picture_backend.model.vo.space.SpaceVO;
import com.sunyujie.picture_backend.service.PictureService;
import com.sunyujie.picture_backend.service.SpaceService;
import com.sunyujie.picture_backend.utils.ResultUtil;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;
    
    @PostMapping("/queryList")
    public BaseResponse<Page<SpaceVO>> queryList(@RequestBody QuerySpaceRequest  querySpaceRequest) {
        return ResultUtil.success(spaceService.querySpaceList(querySpaceRequest), "查询成功");
    }

    @PostMapping("/create")
    public BaseResponse<Long> create(@RequestBody SaveSpaceRequest createSpaceRequest) {
        return ResultUtil.success(spaceService.addSpace(createSpaceRequest), "创建成功");
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody SaveSpaceRequest updateSpaceRequest) {
        return ResultUtil.success(spaceService.updateSpace(updateSpaceRequest), "更新成功");
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteSpaceRequest) {
        return ResultUtil.success(spaceService.deleteSpace(deleteSpaceRequest), "删除成功");
    }

    @GetMapping("/queryById")
    public BaseResponse<SpaceVO> queryById(@RequestParam(required = false) Long id,
            @RequestParam(required = false) Integer spaceType) {
        return ResultUtil.success(spaceService.querySpaceById(id, spaceType), "查询成功");
    }

    @PostMapping("/queryMylist")
    public BaseResponse<List<SpaceVO>> queryMyList() {
        return ResultUtil.success(spaceService.queryMySpaceList(), "查询成功");
    }
}
