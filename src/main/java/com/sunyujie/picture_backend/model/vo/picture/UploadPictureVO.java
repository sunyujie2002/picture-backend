package com.sunyujie.picture_backend.model.vo.picture;

import lombok.Data;
import java.io.Serializable;

@Data
public class UploadPictureVO implements Serializable {

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url（为空表示无需缩略图，前端直接使用 url）
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;
    
    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;
    
    /**
     * 图片颜色
     */
    private String picColor;
    
    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;
}
