package com.sunyujie.picture_backend.model.dto.picture;

import lombok.Data;

@Data
public class BatchUploadPictureRequest {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 图片前缀
     */
    private String prefix;

    /**
     * 分类id
     */
    private Long categoryId;
}
