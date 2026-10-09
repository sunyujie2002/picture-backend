package com.sunyujie.picture_backend.model.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum implements ValueEnum<String> {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String label;
    private final String value;

    UserRoleEnum(String label, String value) {
        this.label = label;
        this.value = value;
    }
}
