package com.sunyujie.picture_backend.collab.model.enums;

import lombok.Getter;

/**
 * 画布操作类型
 * <p>
 * 采用「对象级并发」模型：每个画布元素携带全局唯一 targetId，
 * 不同元素的并发操作互不影响；同一元素按服务端序号后写覆盖先写。
 */
@Getter
public enum EditActionEnum {

    /** 新增元素（文字、线条、矩形等） */
    ADD("add"),

    /** 更新元素属性：移动、缩放、旋转、改色、改内容 */
    UPDATE("update"),

    /** 删除元素 */
    REMOVE("remove"),

    /** 层级调整：上移 / 下移 / 置顶 / 置底 */
    BRING("bring"),

    /** 整图操作：画布尺寸变化、背景裁剪等 */
    CANVAS("canvas"),

    /**
     * 整图滤镜：灰度 / 黑白 / 亮度 / 对比度 / 饱和度 / 模糊
     * <p>
     * 载荷是一组可调参数（而不是 fabric 的滤镜实例）：
     * 滤镜实例含 WebGL 纹理与函数，无法 JSON 化，而参数既能广播也能直接用于撤销。
     */
    FILTER("filter"),

    /** 撤销（作用在一个已有操作上，用 targetOpId 指向被撤销的操作） */
    UNDO("undo");

    private final String value;

    EditActionEnum(String value) {
        this.value = value;
    }

    public static EditActionEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (EditActionEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
