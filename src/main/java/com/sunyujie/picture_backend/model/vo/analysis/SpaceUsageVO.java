package com.sunyujie.picture_backend.model.vo.analysis;

import lombok.Data;

/**
 * 空间用量分析 VO（空间使用排行）
 *
 * 注意：只统计使用内存（totalSize），不统计图片数量——
 * 图片数量是防止用户上传大量小图片的隐藏业务字段，不作为分析维度暴露。
 */
@Data
public class SpaceUsageVO {
    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间类型（0 个人 / 1 团队）
     */
    private Integer spaceType;

    /**
     * 空间创建者用户名
     */
    private String userName;

    /**
     * 已用容量（字节）
     */
    private Long totalSize;

    /**
     * 容量上限（字节），用于前端算使用率
     */
    private Long maxSize;
}
