package com.sunyujie.picture_backend.model.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
/**
 * Table: tag
 */
@Data
@TableName("tag")
public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    @TableField("tag_name")
    private String tagName;

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
     * 是否删除（NULL=未删除，有值=已删除）
     */
    @TableField("delete_time")
    @TableLogic
    private LocalDateTime deleteTime;
}
