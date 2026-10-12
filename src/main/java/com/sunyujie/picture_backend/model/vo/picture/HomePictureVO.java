package com.sunyujie.picture_backend.model.vo.picture;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class HomePictureVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url（为空时前端使用 url）
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片颜色
     */
    private String picColor;
    
    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private Long categoryId;

    /**
     * 标签
     */
    private List<Long> tags;
    
    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

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

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建用户名称
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
