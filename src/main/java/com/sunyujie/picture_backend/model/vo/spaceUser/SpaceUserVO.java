package com.sunyujie.picture_backend.model.vo.spaceUser;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 空间成员 VO（独立扁平，不继承用户 VO）。
 * <p>
 * 表达"某用户在某空间内的协作信息"：前 4 个字段标识用户，后 4 个字段是空间维度信息。
 * 与 SimpleUserVO 只是"碰巧有几个相似字段"，并非 is-a 关系，故不复用继承；
 * 用户主键明确命名为 userId，避免与关联记录 id / 空间 id 语义混淆。
 */
@Data
public class SpaceUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 空间角色：viewer / editor / admin
     */
    private String spaceRole;

    /**
     * 权限码列表（按空间角色由枚举推导，非数据库存储）
     */
    private List<String> permissions;

    /**
     * 加入空间时间
     */
    private LocalDateTime createTime;
}
