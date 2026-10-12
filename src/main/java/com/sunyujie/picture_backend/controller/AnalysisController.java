package com.sunyujie.picture_backend.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.model.dto.analysis.AnalysisRequest;
import com.sunyujie.picture_backend.model.dto.analysis.UsageTrendRequest;
import com.sunyujie.picture_backend.model.vo.analysis.CategoryAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.SizeAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.SpaceUsageVO;
import com.sunyujie.picture_backend.model.vo.analysis.TagAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.UsageTrendVO;
import com.sunyujie.picture_backend.service.AnalysisService;
import com.sunyujie.picture_backend.utils.ResultUtil;

/**
 * 分析控制器
 *
 * 权限（AnalysisServiceImpl#checkAnalysisRequest）：
 * - PUBLIC_SPACE / ALL_SPACE：仅系统管理员
 * - SINGLE_SPACE：个人空间仅创建者；团队空间仅空间管理员与编辑者（viewer 拒绝）
 */
@RestController
@RequestMapping("/analysis")
public class AnalysisController {
    @Resource
    private AnalysisService analysisService;
    /**
     * 分类分析
     */
    @PostMapping("/category")
    public BaseResponse<List<CategoryAnalysisVO>> categoryAnalysis(@RequestBody AnalysisRequest analysisRequest) {
        return ResultUtil.success(analysisService.categoryAnalysis(analysisRequest), "分类分析查询成功");
    }

    /**
     * 标签分析
     */
    @PostMapping("/tag")
    public BaseResponse<List<TagAnalysisVO>> tagAnalysis(@RequestBody AnalysisRequest analysisRequest) {
        return ResultUtil.success(analysisService.tagAnalysis(analysisRequest), "标签分析查询成功");
    }

    /**
     * 图片大小分析（按体积分段）
     */
    @PostMapping("/size")
    public BaseResponse<List<SizeAnalysisVO>> sizeAnalysis(@RequestBody AnalysisRequest analysisRequest) {
        return ResultUtil.success(analysisService.sizeAnalysis(analysisRequest), "图片大小分析查询成功");
    }

    /**
     * 用户上传行为分析（按天统计，近 lastDays 天）
     */
    @PostMapping("/trend")
    public BaseResponse<List<UsageTrendVO>> usageTrendAnalysis(@RequestBody UsageTrendRequest usageTrendRequest) {
        return ResultUtil.success(analysisService.usageTrendAnalysis(usageTrendRequest), "上传行为分析查询成功");
    }

    /**
     * 空间用量分析（排行，仅管理员；只统计使用内存，不统计图片数量）
     */
    @PostMapping("/space")
    public BaseResponse<List<SpaceUsageVO>> spaceUsageAnalysis() {
        return ResultUtil.success(analysisService.spaceUsageAnalysis(), "空间用量分析查询成功");
    }
}
