package com.sunyujie.picture_backend.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 空间用户关联表（多对多：一个空间多个成员，一个用户加入多个空间）
 * <p>
 * 仅团队空间（space_type = 1）产生关联记录；个人空间不写关联，权限只认空间创建者。
 * id 用数据库自增（关联表无业务含义，且方便存量数据回填）；
 * 成员移除走逻辑删除（delete_time），重新邀请直接新增一条记录（不复用历史行）；
 * 唯一索引 (space_id, user_id, delete_time)：活跃行 delete_time 相同，
 * 已删除行删除时间各不相同，因此移除后重新加入不会撞索引。
 */
@Data
@TableName("space_user")
public class SpaceUser {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 空间 id
     */
    @TableField("space_id")
    private Long spaceId;

    /**
     * 用户 id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 空间角色：viewer / editor / admin
     */
    @TableField("space_role")
    private String spaceRole;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：null-正常，时间值-移除时间
     */
    @TableField("delete_time")
    @TableLogic
    private LocalDateTime deleteTime;
}
