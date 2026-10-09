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
 * Table: user
 */
@Data
@TableName("user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    @TableField("user_account")
    private String userAccount;

    /**
     * 密码
     */
    @TableField("user_password")
    private String userPassword;

    /**
     * 用户昵称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 用户头像
     */
    @TableField("user_avatar")
    private String userAvatar;

    /**
     * 用户简介
     */
    @TableField("user_profile")
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    @TableField("user_role")
    private String userRole;

    /**
     * 编辑时间
     */
    @TableField("edit_time")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic()
    @TableField("delete_time")
    private LocalDateTime deleteTime;
}
