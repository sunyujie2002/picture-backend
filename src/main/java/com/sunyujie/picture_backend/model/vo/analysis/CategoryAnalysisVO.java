package com.sunyujie.picture_backend.model.vo.analysis;

import lombok.Data;

/**
 * 分类分析VO
 */
@Data
public class CategoryAnalysisVO {
    /**
     * 分类名称
     */
    private String categoryName;
    /**
     * 图片数量
     */
    private Long pictureCount;
    /**
     * 总大小
     */
    private Long totalSize;
}
