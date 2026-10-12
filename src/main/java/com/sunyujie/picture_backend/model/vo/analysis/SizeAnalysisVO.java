package com.sunyujie.picture_backend.model.vo.analysis;

import lombok.Data;

/**
 * 图片大小分析 VO（按体积分段统计）
 */
@Data
public class SizeAnalysisVO {
    /**
     * 大小分段标识（下界含、上界不含，最后一段含）：
     * LT_100KB（小于 100KB，缩略图级）
     * 100KB_500KB（100KB ~ 500KB，符合 Web 性能预算）
     * 500KB_2MB（500KB ~ 2MB，高清图片）
     * 2MB_5MB（2MB ~ 5MB，大图，建议压缩）
     * GTE_5MB（大于等于 5MB，超大原图）
     */
    private String sizeRange;

    /**
     * 图片数量
     */
    private Long pictureCount;

    /**
     * 该分段的总大小（字节）
     */
    private Long totalSize;
}
