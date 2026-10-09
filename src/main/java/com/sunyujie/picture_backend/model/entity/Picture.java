package com.sunyujie.picture_backend.model.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * Table: picture
 */
@Data
@TableName("picture")
public class Picture implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     * 更新时始终写入该字段（含 null）：换图后新图无缩略图时，需要能显式置空
     */
    @TableField(value = "thumbnail_url", updateStrategy = FieldStrategy.IGNORED)
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    @TableField("pic_name")
    private String picName;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类 id
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 图片体积
     */
    @TableField("pic_size")
    private Long picSize;

    /**
     * 图片宽度
     */
    @TableField("pic_width")
    private Integer picWidth;

    /**
     * 图片高度
     */
    @TableField("pic_height")
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    @TableField("pic_scale")
    private Double picScale;

    /**
     * 图片格式
     */
    @TableField("pic_format")
    private String picFormat;

    /**
     * 图片感知哈希指纹（以图搜图用）
     */
    @TableField("pic_hash")
    private Long picHash;

    /**
     * 图片关联空间ID
     */
    @TableField("space_id")
    private Long spaceId;

    /**
     * 图片颜色
     */
    @TableField("pic_color")
    private String picColor;

    /**
     * 创建用户 id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    @TableField("review_status")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @TableField("review_message")
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * 审核时间
     */
    @TableField("review_time")
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 编辑时间
     */
    @TableField("edit_time")
    private LocalDateTime editTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField("delete_time")
    @TableLogic 
    private LocalDateTime deleteTime;
}
