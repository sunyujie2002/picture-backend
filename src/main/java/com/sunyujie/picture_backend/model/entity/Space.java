package com.sunyujie.picture_backend.model.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * Table: space
 */
@Data
@TableName("space")
public class Space implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    @TableField("space_name")
    private String spaceName;

    /**
     * 空间类型：0-个人空间 1-团队空间
     */
    @TableField("space_type")
    private Integer spaceType;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @TableField("space_level")
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    @TableField("max_size")
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    @TableField("max_count")
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    @TableField("total_size")
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    @TableField("total_count")
    private Long totalCount;

    /**
     * 创建用户 id
     */
    @TableField("user_id")
    private Long userId;

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
