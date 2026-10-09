package com.sunyujie.picture_backend.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.model.dto.analysis.AnalysisRequest;
import com.sunyujie.picture_backend.model.vo.analysis.CategoryAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.TagAnalysisVO;
import com.sunyujie.picture_backend.service.AnalysisService;
import com.sunyujie.picture_backend.utils.ResultUtil;

/**
 * 分析控制器
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
}
