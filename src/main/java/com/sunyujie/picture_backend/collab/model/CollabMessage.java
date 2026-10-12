package com.sunyujie.picture_backend.collab.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协作消息信封：WebSocket 与 Redis Stream 共用的传输结构
 *
 * @param type    消息类型，取值见 CollabMessageTypeEnum
 * @param roomId  房间标识（这里直接复用图片 id，字符串形态）
 * @param payload 业务载荷，随 type 不同而不同：
 *                OP → EditOperation；SNAPSHOT → EditRoomState；MEMBERS → List&lt;RoomMember&gt;；ERROR → 文本
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollabMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;

    private String roomId;

    /**
     * 发起者用户 id（下行消息里用来标记"这条操作是谁做的"，发起端据此跳过回显）
     */
    private String userId;

    private Object payload;

    /**
     * 服务端时间戳，仅做展示与排序参考，权威顺序以 seq 为准
     */
    private Long timestamp;

    public static CollabMessage of(String type, String roomId, Object payload) {
        CollabMessage message = new CollabMessage();
        message.setType(type);
        message.setRoomId(roomId);
        message.setPayload(payload);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
}
