package com.sunyujie.picture_backend.model.enums;

/**
 * 带显式值的枚举通用接口
 * <p>
 * 让枚举持有一个与声明顺序（ordinal）无关、持久化到数据库的值。
 * value 的类型由各枚举自行指定（Integer / String 等）。
 *
 * @param <T> 值的类型
 */
public interface ValueEnum<T> {

    /**
     * 获取枚举对应的持久化值
     *
     * @return 值
     */
    T getValue();
}
