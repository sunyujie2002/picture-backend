package com.sunyujie.picture_backend.collab.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房间内的在线成员
 * <p>
 * color 由服务端按用户 id 稳定散列生成，保证同一用户在不同客户端显示的协作者色块颜色一致。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 id（字符串）
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 头像标识色（#rrggbb）
     */
    private String color;

    /**
     * 空间角色：viewer / editor / admin；公共图入库时为空
     */
    private String spaceRole;

    /**
     * 是否是协作者本人
     */
    private Boolean self;

    /**
     * 进入房间时间
     */
    private Long joinedAt;
}
