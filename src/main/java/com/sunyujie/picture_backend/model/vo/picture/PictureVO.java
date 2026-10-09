package com.sunyujie.picture_backend.model.vo.picture;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PictureVO implements Serializable {
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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 审核状态
     */
    private Integer reviewStatus;

    /**
     * 审核状态描述
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
}
