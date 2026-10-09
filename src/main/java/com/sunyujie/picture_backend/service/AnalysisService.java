package com.sunyujie.picture_backend.service;

import java.util.List;

import com.sunyujie.picture_backend.model.dto.analysis.AnalysisRequest;
import com.sunyujie.picture_backend.model.vo.analysis.CategoryAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.TagAnalysisVO;

public interface AnalysisService {
    /**
     * 分类分析
     * @return 分类分析VO列表
     */
    List<CategoryAnalysisVO> categoryAnalysis(AnalysisRequest analysisRequest);

    /**
     * 标签分析
     * @return 标签分析VO列表
     */
    List<TagAnalysisVO> tagAnalysis(AnalysisRequest analysisRequest);
}
