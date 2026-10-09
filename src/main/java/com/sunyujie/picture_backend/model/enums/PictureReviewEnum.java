package com.sunyujie.picture_backend.model.enums;

/**
 * 图片审核状态枚举
 */
import lombok.Getter;

@Getter
public enum PictureReviewEnum implements ValueEnum<Integer> {
    /**
     * 待审核
     */
    PENDING(0, "待审核"),
    /**
     * 通过
     */
    PASS(1, "审核通过"),
    /**
     * 拒绝
     */
    REJECT(2, "审核拒绝");

    private final String label;
    private final Integer value;

    PictureReviewEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
