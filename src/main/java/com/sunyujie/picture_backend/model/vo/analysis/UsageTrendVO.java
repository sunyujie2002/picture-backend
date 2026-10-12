package com.sunyujie.picture_backend.model.vo.analysis;

import java.util.List;

import lombok.Data;

/**
 * 用户上传行为分析 VO（按统计周期统计上传数量）
 */
@Data
public class UsageTrendVO {
    /**
     * 统计周期：本周 / 本月为 yyyy-MM-dd，本年为 yyyy-MM
     */
    private String statDate;

    /**
     * 该周期内的上传图片数量
     */
    private Long pictureCount;

    /**
     * 该周期内各用户的上传明细，按数量倒序。点击折线上的点时前端直接展示
     */
    private List<UserUploadStatVO> userStats;
}
