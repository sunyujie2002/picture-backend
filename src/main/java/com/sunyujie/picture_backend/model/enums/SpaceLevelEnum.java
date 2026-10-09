package com.sunyujie.picture_backend.model.enums;

import lombok.Getter;

/**
 * 空间级别枚举
 */
@Getter
public enum SpaceLevelEnum implements ValueEnum<Integer> {
    /**
     * 普通版
     */
    BASIC(0, "普通版", 5 * 1024 * 1024L * 1024L, 5 * 20000L),
    /**
     * 专业版
     */
    PRO(1, "专业版", 20 * 1024 * 1024L * 1024L, 20 * 20000L),
    /**
     * 旗舰版
     */
    PREMIUM(2, "旗舰版", 50 * 1024 * 1024L * 1024L, 50 * 20000L);

    /**
     * 空间级别值
     */
    private final Integer value;
    private final String label;
    private final Long size;
    private final Long count;
    public static final Long COUNT_UNIT = 20000L;

    SpaceLevelEnum(Integer value, String label, Long size, Long count) {
        this.value = value;
        this.label = label;
        this.size = size;
        this.count = count;
    }
}
