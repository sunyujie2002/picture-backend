package com.sunyujie.picture_backend.collab.controller;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sunyujie.picture_backend.collab.manager.EditAuthManager;
import com.sunyujie.picture_backend.collab.model.dto.EditAuthResult;
import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.utils.ResultUtil;

/**
 * 图片协作编辑相关查询接口
 * <p>
 * 目前只提供一个能力：查询当前用户对某张图片的协作编辑权限。
 * 前端用它决定「图片编辑」按钮是否渲染 —— 由服务端裁决，避免前端拼错规则把不该露的入口露出去
 * （尤其是团队空间的 viewer）。
 */
@RestController
@RequestMapping("/picture/collab")
public class PictureCollabController {

    @Resource
    private EditAuthManager editAuthManager;

    /**
     * 查询当前用户对指定图片的协作编辑权限
     *
     * @param pictureId 图片 id
     * @return 鉴权结果，无权限时抛 NO_AUTH_ERROR
     */
    @GetMapping("/auth")
    public BaseResponse<EditAuthResult> queryEditAuth(@RequestParam String pictureId) {
        Long loginUserId = UserContext.getLoginUserId();
        EditAuthResult result = editAuthManager.authPictureEdit(Long.valueOf(pictureId), loginUserId,
                UserContext.getRole());
        return ResultUtil.success(result, "查询成功");
    }
}
