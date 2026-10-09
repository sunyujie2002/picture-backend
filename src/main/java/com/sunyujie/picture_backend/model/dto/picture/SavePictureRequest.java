package com.sunyujie.picture_backend.model.dto.picture;

import java.util.List;
import lombok.Data;

@Data
public class SavePictureRequest {

    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 空间 id：传了操作指定空间（团队成员可传团队空间 id）；不传默认当前用户的个人空间
     */
    private Long spaceId;

    /**
     * 分类 id
     */
    private Long categoryId;

    /**
     * 图片标签
     */
    private List<Long> tagIds;

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
     * 图片颜色
     */
    private String picColor;
}
