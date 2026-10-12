package com.sunyujie.picture_backend.model.enums;

import lombok.Getter;

/**
 * 上传行为分析的时间范围枚举
 *
 * 只有上传行为分析涉及时间概念，分类 / 标签 / 大小分析一律统计建库以来的全量数据
 */
@Getter
public enum TrendTimeRangeEnum implements ValueEnum<String> {
    WEEK("WEEK", "本周", "按天统计本周一至周日"),
    MONTH("MONTH", "本月", "按天统计本月 1 号至月末"),
    YEAR("YEAR", "本年", "按月统计本年 1 至 12 月");

    private final String value;
    private final String label;
    private final String desc;

    TrendTimeRangeEnum(String value, String label, String desc) {
        this.value = value;
        this.label = label;
        this.desc = desc;
    }

    /**
     * 按 value 取枚举，未传或非法值默认本周
     */
    public static TrendTimeRangeEnum of(String value) {
        for (TrendTimeRangeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return WEEK;
    }
}
