package com.sunyujie.picture_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.model.dto.picture.SavePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchUpdatePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchDeletePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchUploadPictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.QueryHomePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.QueryPictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.ReviewPictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.SearchPictureByPictureRequest;
import com.sunyujie.picture_backend.model.vo.picture.HomePictureVO;
import com.sunyujie.picture_backend.model.vo.picture.PictureVO;
import com.sunyujie.picture_backend.model.vo.picture.UploadPictureVO;
import com.sunyujie.picture_backend.service.PictureService;
import com.sunyujie.picture_backend.utils.MultiLevelCacheUtil;
import com.sunyujie.picture_backend.utils.ResultUtil;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private PictureService pictureService;

    @PostMapping("/upload")
    public BaseResponse<UploadPictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile) {
        return ResultUtil.success(pictureService.uploadPicture(multipartFile), "上传成功");
    }

    @GetMapping("/uploadByUrl")
    public BaseResponse<UploadPictureVO> uploadPictureByUrl(@RequestParam String fileUrl) {
        return ResultUtil.success(pictureService.uploadByUrl(fileUrl), "上传成功");
    }

    @PostMapping("/uploadByBatch")
    public BaseResponse<String> uploadPictureByBatch(
            @RequestBody BatchUploadPictureRequest batchUploadPictureRequest) {
        String result = pictureService.uploadPictureByBatch(batchUploadPictureRequest);
        return ResultUtil.success(null, result);
    }

    @PostMapping("/{scope}/add")
    public BaseResponse<Long> addPicture(@PathVariable String scope,
            @RequestBody SavePictureRequest savePictureRequest) {
        // scope 只允许 public / private，其他值直接拒绝，避免默认掉进公共图逻辑
        ThrowUtil.throwIf(!"public".equals(scope) && !"private".equals(scope),
                ErrorCode.PARAMS_ERROR, "图片范围不正确");
        Long id = "private".equals(scope)
                ? pictureService.addPrivatePicture(savePictureRequest)
                : pictureService.addPublicPicture(savePictureRequest);
        return ResultUtil.success(id, "图片添加成功");
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody SavePictureRequest savePictureRequest) {
        return ResultUtil.success(pictureService.updatePicture(savePictureRequest), "图片更新成功");
    }

    @PostMapping("/batch/update")
    public BaseResponse<Boolean> batchUpdatePicture(
            @Valid @RequestBody BatchUpdatePictureRequest batchUpdatePictureRequest) {
        return ResultUtil.success(pictureService.batchUpdatePicture(batchUpdatePictureRequest), "批量更新成功");
    }

    @PostMapping("/batch/delete")
    public BaseResponse<Boolean> batchDeletePicture(
            @Valid @RequestBody BatchDeletePictureRequest batchDeletePictureRequest) {
        return ResultUtil.success(pictureService.batchDeletePicture(batchDeletePictureRequest), "批量删除成功");
    }

    @PostMapping("/public/queryList")
    public BaseResponse<Page<PictureVO>> queryPublicPictureList(@RequestBody QueryPictureRequest queryPictureRequest) {
        return ResultUtil.success(pictureService.queryPublicPictureList(queryPictureRequest), "查询成功");
    }

    @PostMapping("/private/queryList")
    public BaseResponse<Page<PictureVO>> queryPrivatePictureList(
            @RequestBody QueryHomePictureRequest queryHomePictureRequest) {
        return ResultUtil.success(pictureService.queryPrivatePictureList(queryHomePictureRequest), "查询成功");
    }

    /**
     * 管理员审核列表：公共图库中非管理员用户上传的图片
     */
    @PostMapping("/review/queryList")
    public BaseResponse<Page<PictureVO>> queryReviewPictureList(
            @RequestBody QueryPictureRequest queryPictureRequest) {
        return ResultUtil.success(pictureService.queryReviewPictureList(queryPictureRequest), "查询成功");
    }

    @PostMapping("/home/queryList")
    public BaseResponse<Page<HomePictureVO>> queryHomePictureList(
            @RequestBody QueryHomePictureRequest queryHomePictureRequest) {
        return ResultUtil.success(pictureService.queryHomePictureList(queryHomePictureRequest), "查询成功");
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest) {
        return ResultUtil.success(pictureService.deletePicture(deleteRequest), "图片删除成功");
    }

    @GetMapping("/queryById")
    public BaseResponse<HomePictureVO> queryPictureById(@RequestParam Long id) {
        // 第1个参数：缓存 key（字符串，带业务前缀）项目名称:模块名称:接口名称:参数值
        // 第2个参数：必须是 lambda () -> ...，只在两级缓存都未命中时才执行；
        // 直接写 pictureService.queryPictureById(id) 会在调用前就执行查询，缓存形同虚设
        HomePictureVO HomePictureVO = MultiLevelCacheUtil.getWithCache(
                "picture:picture:queryById:" + id,
                () -> pictureService.queryPictureById(id));
        return ResultUtil.success(HomePictureVO, "查询成功");
    }

    @PostMapping("/search/picture")
    public BaseResponse<List<HomePictureVO>> searchPictureByPicture(
            @Valid @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        List<HomePictureVO> pictureList = pictureService
                .searchPictureByPicture(searchPictureByPictureRequest.getPictureId());
        return ResultUtil.success(pictureList, "搜索成功");
    }

    @PostMapping("/review")
    public BaseResponse<Boolean> reviewPicture(@RequestBody ReviewPictureRequest reviewPictureRequest) {
        return ResultUtil.success(pictureService.reviewPicture(reviewPictureRequest), "图片审核成功");
    }

    @GetMapping("/download")
    public void downloadPicture(@RequestParam Long id, HttpServletResponse response) {
        pictureService.downloadPicture(id, response);
    }
}
