package com.sunyujie.picture_backend.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.model.dto.tag.AddTagRequest;
import com.sunyujie.picture_backend.model.dto.tag.UpdateTagRequest;
import com.sunyujie.picture_backend.model.entity.Tag;
import com.sunyujie.picture_backend.model.vo.tag.TagVO;
import com.sunyujie.picture_backend.service.TagService;
import com.sunyujie.picture_backend.utils.ResultUtil;

/**
 * 标签 Controller
 */
@RestController
@RequestMapping("/tag")
public class TagController {

    @Resource
    private TagService tagService;

    // ==================== 普通用户 ====================

    @GetMapping("/queryList")
    public BaseResponse<List<TagVO>> queryTagList() {
        return ResultUtil.success(tagService.queryTagList(), "查询成功");
    }

    // ==================== 管理员（service 层 @AuthCheck 强制校验） ====================

    @GetMapping("/admin/queryList")
    public BaseResponse<List<Tag>> queryAdminTagList() {
        return ResultUtil.success(tagService.queryAdminTagList(), "查询成功");
    }

    @PostMapping("/admin/add")
    public BaseResponse<Long> addTag(@Valid @RequestBody AddTagRequest addTagRequest) {
        Long id = tagService.addTag(addTagRequest);
        return ResultUtil.success(id, "标签添加成功");
    }

    @PostMapping("/admin/update")
    public BaseResponse<Boolean> updateTag(
            @Valid @RequestBody UpdateTagRequest updateTagRequest) {
        return ResultUtil.success(tagService.updateTag(updateTagRequest), "标签更新成功");
    }

    @PostMapping("/admin/delete")
    public BaseResponse<Boolean> deleteTag(@RequestBody DeleteRequest deleteRequest) {
        return ResultUtil.success(tagService.deleteTag(deleteRequest), "标签删除成功");
    }
}
