package com.sunyujie.picture_backend.service;

import java.util.List;

import com.sunyujie.picture_backend.model.dto.analysis.AnalysisRequest;
import com.sunyujie.picture_backend.model.dto.analysis.UsageTrendRequest;
import com.sunyujie.picture_backend.model.vo.analysis.CategoryAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.SizeAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.SpaceUsageVO;
import com.sunyujie.picture_backend.model.vo.analysis.TagAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.UsageTrendVO;

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

    /**
     * 图片大小分析：按体积分段（<100KB / 100KB~1MB / >1MB）统计数量与总大小
     * @return 大小分段VO列表
     */
    List<SizeAnalysisVO> sizeAnalysis(AnalysisRequest analysisRequest);

    /**
     * 用户上传行为分析：按天统计上传数量（近 lastDays 天，默认 30，最大 365），
     * 周 / 月趋势由前端按日数据聚合
     * @return 按日统计VO列表
     */
    List<UsageTrendVO> usageTrendAnalysis(UsageTrendRequest usageTrendRequest);

    /**
     * 空间用量分析（排行）：只统计使用内存（totalSize），不统计图片数量，
     * 图片数量是防止用户上传大量小图片的隐藏业务字段
     * @return 按已用容量倒序的空间用量列表（仅管理员）
     */
    List<SpaceUsageVO> spaceUsageAnalysis();
}
