package com.sunyujie.picture_backend.model.enums;

/**
 * 分析范围枚举
 */
import lombok.Getter;

@Getter
public enum AnalysisScopeEnum implements ValueEnum<String> {
    PUBLIC_SPACE("PUBLIC_SPACE", "公共图库"),
    ALL_SPACE("ALL_SPACE", "所有用户空间"),
    SINGLE_SPACE("SINGLE_SPACE", "用户个人空间");
    
    private final String value;
    private final String label;
    
    AnalysisScopeEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
