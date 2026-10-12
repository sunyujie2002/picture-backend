package com.sunyujie.picture_backend.collab.model.enums;

import lombok.Getter;

/**
 * 协作消息类型
 * <p>
 * 上行（客户端 → 服务端）与下行（服务端 → 客户端）共用一套枚举，
 * 通过 direction 区分，便于按类型做路由分发。
 */
@Getter
public enum CollabMessageTypeEnum {

    // ------- 上行：客户端 → 服务端 -------

    /** 提交一次画布操作 */
    OP("op", "上行"),

    /** 光标位置（即时同步、不落库、不进操作日志） */
    CURSOR("cursor", "上行"),

    /** 心跳保活 */
    PING("ping", "上行"),

    /** 成员已把画布保存为新的图片（走 HTTP 更新接口成功后上报） */
    SAVE("save", "上行"),

    // ------- 下行：服务端 → 客户端 -------

    /** 加入房间时的全量快照：基础图片 + 已固化的操作日志 */
    SNAPSHOT("snapshot", "下行"),

    /** 服务端定序后广播的操作 */
    OP_BROADCAST("op_broadcast", "下行"),

    /** 房间成员变化 */
    MEMBERS("members", "下行"),

    /** 其他成员的光标移动 */
    CURSOR_BROADCAST("cursor_broadcast", "下行"),

    /** 已有成员完成保存，图片已被覆盖 */
    SAVED("saved", "下行"),

    /** 操作被拒绝或参数有误 */
    ERROR("error", "下行"),

    /** 心跳应答 */
    PONG("pong", "下行");

    private final String value;

    private final String direction;

    CollabMessageTypeEnum(String value, String direction) {
        this.value = value;
        this.direction = direction;
    }

    /**
     * 按值反查枚举，非法值返回 null
     */
    public static CollabMessageTypeEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (CollabMessageTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
