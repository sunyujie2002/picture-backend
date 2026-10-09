package com.sunyujie.picture_backend.utils;

import java.util.Objects;

import com.sunyujie.picture_backend.model.enums.ValueEnum;

/**
 * 枚举工具类：按枚举的显式值（ValueEnum.getValue）反查枚举常量
 */
public class EnumUtil {

    /**
     * 根据持久化值获取对应的枚举常量
     *
     * @param value     持久化值
     * @param enumClass 枚举类型
     * @param <T>       值的类型
     * @param <E>       枚举类型
     * @return 匹配的枚举常量；未找到（或 value 为 null）时返回 null
     */
    public static <T, E extends Enum<E> & ValueEnum<T>> E getEnumByValue(T value, Class<E> enumClass) {
        if (value == null) {
            return null;
        }
        for (E enumItem : enumClass.getEnumConstants()) {
            // 用 Objects.equals 比较，兼容 value 为引用类型（Integer/String）的情况
            if (Objects.equals(enumItem.getValue(), value)) {
                return enumItem;
            }
        }
        return null;
    }
}
