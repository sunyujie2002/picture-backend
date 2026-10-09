package com.sunyujie.picture_backend.model.vo.analysis;

import lombok.Data;

/**
 * 标签分析VO
 */
@Data
public class TagAnalysisVO {
     /**
     * 标签名称
     */
    private String tagName;
    /**
     * 图片数量
     */
    private Long pictureCount;
    /**
     * 总大小
     */
    private Long totalSize;
}
