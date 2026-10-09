package com.sunyujie.picture_backend.model.enums;

import lombok.Getter;

/**
 * 空间类型枚举
 */
@Getter
public enum SpaceTypeEnum implements ValueEnum<Integer> {
    /**
     * 个人空间
     */
    PERSONAL(0, "个人空间"),
    /**
     * 团队空间
     */
    TEAM(1, "团队空间");

    /**
     * 空间类型值
     */
    private final Integer value;

    private final String label;

    SpaceTypeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
