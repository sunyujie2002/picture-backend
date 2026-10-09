package com.sunyujie.picture_backend.model.vo.space;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 空间VO
 */
@Data
public class SpaceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间类型：0-个人空间 1-团队空间
     */
    private Integer spaceType;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    // /**
    //  * 空间图片的最大数量
    //  */
    // private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建的用户名
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 当前登录用户在该空间的角色：viewer / editor / admin
     * （仅"我的空间列表"回填：自己创建的空间为 admin，加入的空间取关联表角色）
     */
    private String spaceRole;
}
