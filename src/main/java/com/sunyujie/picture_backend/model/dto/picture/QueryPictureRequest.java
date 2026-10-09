package com.sunyujie.picture_backend.model.dto.picture;

import java.util.List;

import com.sunyujie.picture_backend.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class QueryPictureRequest extends PageRequest {
    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id：私有图片列表按此定位空间；不传默认当前用户的个人空间
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
     * 图片格式
     */
    private String picFormat;

    /**
     * 主色调（十六进制，支持 #RRGGBB / 0xRRGGBB）
     * 传入后按颜色相似度排序：与该颜色越接近的图片越靠前
     */
    private String picColor;

    /**
     * 图片审核状态
     */
    private Integer reviewStatus;
}
