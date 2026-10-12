package com.sunyujie.picture_backend.collab.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunyujie.picture_backend.collab.manager.EditRoomManager;
import com.sunyujie.picture_backend.collab.manager.EditStateManager;
import com.sunyujie.picture_backend.collab.model.CollabMessage;
import com.sunyujie.picture_backend.collab.model.EditOperation;
import com.sunyujie.picture_backend.collab.model.EditRoomState;
import com.sunyujie.picture_backend.collab.model.RoomMember;
import com.sunyujie.picture_backend.collab.model.enums.CollabMessageTypeEnum;
import com.sunyujie.picture_backend.collab.model.enums.EditActionEnum;
import com.sunyujie.picture_backend.collab.mq.CollabStreamListener;
import com.sunyujie.picture_backend.collab.mq.CollabStreamProducer;

import lombok.extern.slf4j.Slf4j;

/**
 * 图片协作编辑 WebSocket 主处理器
 * <p>
 * 数据流（刻意设计成单一路径）：
 * <pre>
 *   任意成员操作 ──► 服务端定序(nextSeq) ──► 写草稿状态 ──► Redis Stream
 *                                                            │
 *                      本实例所有会话 ◄── 消费组回调 ◄────────┘
 * </pre>
 * 发起者自己的操作同样要绕一圈队列才回本地，而不是直接乐观应用。
 * 这样所有客户端看到的操作顺序完全一致，状态必然收敛，不会出现"本地先跳、回包再纠偏"的抖动。
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler implements CollabStreamListener {

    @Resource
    private EditRoomManager editRoomManager;

    @Resource
    private EditStateManager editStateManager;

    @Resource
    private CollabStreamProducer collabStreamProducer;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * session 属性 key：房间 id（图片 id）
     */
    private static final String ATTR_ROOM_ID = "roomId";

    private static final String ATTR_USER_ID = "userId";

    private static final String ATTR_USER_NAME = "userName";

    private static final String ATTR_SPACE_ROLE = "spaceRole";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = (String) session.getAttributes().get(ATTR_ROOM_ID);
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        String userName = (String) session.getAttributes().get(ATTR_USER_NAME);
        String spaceRole = (String) session.getAttributes().get(ATTR_SPACE_ROLE);

        RoomMember member = new RoomMember();
        member.setUserId(userId);
        member.setUserName(userName);
        member.setSpaceRole(spaceRole);
        member.setColor(editRoomManager.colorFor(userId));
        member.setSelf(Boolean.FALSE);
        member.setJoinedAt(System.currentTimeMillis());
        editRoomManager.join(roomId, session, member);

        // 1. 先给新成员下发房间草稿快照（含别人的编辑成果）
        EditRoomState state = editStateManager.getRoomState(roomId);
        state.setRoomId(roomId);
        sendTo(session, CollabMessage.of(CollabMessageTypeEnum.SNAPSHOT.getValue(), roomId, state));

        // 2. 再下发成员列表（自己的那条标记 self，前端据此不渲染自己的光标）
        List<RoomMember> members = editRoomManager.listMembers(roomId);
        Map<String, Object> selfPayload = new HashMap<>(4);
        selfPayload.put("members", members);
        selfPayload.put("selfId", userId);
        sendTo(session, CollabMessage.of(CollabMessageTypeEnum.MEMBERS.getValue(), roomId, selfPayload));

        // 3. 通知房间内其他人有人进来
        broadcastMembers(roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = (String) session.getAttributes().get(ATTR_ROOM_ID);
        String payload = message.getPayload();

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            sendTo(session, CollabMessage.of(CollabMessageTypeEnum.ERROR.getValue(), roomId, "消息格式有误"));
            return;
        }
        String type = (String) body.get("type");
        CollabMessageTypeEnum typeEnum = CollabMessageTypeEnum.getByValue(type);
        if (typeEnum == null) {
            sendTo(session, CollabMessage.of(CollabMessageTypeEnum.ERROR.getValue(), roomId, "未知的消息类型"));
            return;
        }
        switch (typeEnum) {
            case OP -> handleOperation(session, roomId, body.get("payload"));
            case CURSOR -> handleCursor(session, roomId, body.get("payload"));
            case SAVE -> handleSave(roomId, body.get("payload"));
            case PING -> sendTo(session, CollabMessage.of(CollabMessageTypeEnum.PONG.getValue(), roomId, ""));
            default -> sendTo(session, CollabMessage.of(CollabMessageTypeEnum.ERROR.getValue(), roomId,
                    "该消息类型不支持上行: " + type));
        }
    }

    /**
     * 处理一次画布操作：定序 → 落草稿 → 投递队列
     */
    private void handleOperation(WebSocketSession session, String roomId, Object rawPayload) {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        String userName = (String) session.getAttributes().get(ATTR_USER_NAME);
        try {
            EditOperation operation = objectMapper.convertValue(rawPayload, EditOperation.class);
            if (operation == null || operation.getAction() == null) {
                sendTo(session, CollabMessage.of(CollabMessageTypeEnum.ERROR.getValue(), roomId, "操作内容不完整"));
                return;
            }
            EditActionEnum action = EditActionEnum.getByValue(operation.getAction());
            if (action == null) {
                sendTo(session, CollabMessage.of(CollabMessageTypeEnum.ERROR.getValue(), roomId, "不支持的操作类型"));
                return;
            }
            // 重连/重试可能重发同一条操作，按客户端生成的 opId 幂等跳过
            if (editStateManager.opExists(roomId, operation.getOpId())) {
                return;
            }
            // 服务端权威定序：所有客户端都按这个序号重放，顺序一致才有状态收敛
            operation.setSeq(editStateManager.nextSeq(roomId));
            operation.setUserId(userId);
            operation.setUserName(userName);
            operation.setTimestamp(System.currentTimeMillis());
            editStateManager.appendOperation(roomId, operation);

            CollabMessage out = CollabMessage.of(CollabMessageTypeEnum.OP_BROADCAST.getValue(), roomId, operation);
            out.setUserId(userId);
            String recordId = collabStreamProducer.send(out);
            if (recordId == null) {
                // 队列不可用时降级为实例内广播，保证单机场景编辑不中断（多实例会丢跨节点同步，属异常路径）
                log.warn("collab stream unavailable, fallback to local broadcast, roomId = {}", roomId);
                broadcastLocal(roomId, out);
            }
        } catch (Exception e) {
            log.error("collab handle operation error, roomId = {}, userId = {}", roomId, userId, e);
            sendTo(session, CollabMessage.of(CollabMessageTypeEnum.ERROR.getValue(), roomId, "操作处理失败"));
        }
    }

    /**
     * 处理光标移动：不落库、不进队列之外的存储，纯实时提示
     */
    private void handleCursor(WebSocketSession session, String roomId, Object rawPayload) {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        String userName = (String) session.getAttributes().get(ATTR_USER_NAME);
        try {
            Map<String, Object> cursor = new HashMap<>(6);
            if (rawPayload instanceof Map) {
                cursor.putAll((Map<String, Object>) rawPayload);
            }
            cursor.put("userId", userId);
            cursor.put("userName", userName);
            cursor.put("color", editRoomManager.colorFor(userId));
            CollabMessage out = CollabMessage.of(CollabMessageTypeEnum.CURSOR_BROADCAST.getValue(), roomId, cursor);
            out.setUserId(userId);
            collabStreamProducer.send(out);
        } catch (Exception e) {
            log.debug("collab handle cursor error, roomId = {}", roomId);
        }
    }

    /**
     * 处理保存完成上报：草稿已固化到图片，通知房间内所有人同步基础图并清空本地草稿
     */
    private void handleSave(String roomId, Object rawPayload) {
        try {
            // 图片已被覆盖，草稿使命结束；之后所有人看到的"编辑层"从零开始
            editStateManager.clearRoom(roomId);
            CollabMessage out = CollabMessage.of(CollabMessageTypeEnum.SAVED.getValue(), roomId,
                    rawPayload == null ? new HashMap<String, Object>() : rawPayload);
            collabStreamProducer.send(out);
        } catch (Exception e) {
            log.error("collab handle save error, roomId = {}", roomId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = (String) session.getAttributes().get(ATTR_ROOM_ID);
        if (roomId == null) {
            return;
        }
        boolean emptied = editRoomManager.leave(roomId, session);
        broadcastMembers(roomId);
        // 房间空了就丢弃草稿：没人保存过的情况下不应留下脏状态
        if (emptied) {
            editStateManager.clearRoom(roomId);
        }
    }

    /**
     * 队列回调：把持久化到 Stream 的消息广播给本实例内的会话
     * <p>
     * 多实例部署时，每个实例都会收到这条消息，各自只广播给连接在自己身上的会话，天然完成扇出。
     */
    @Override
    public void onCollabMessage(CollabMessage message) {
        broadcastLocal(message.getRoomId(), message);
    }

    private void broadcastLocal(String roomId, CollabMessage message) {
        List<WebSocketSession> sessions = editRoomManager.listSessions(roomId);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendTo(session, message);
            }
        }
    }

    private void broadcastMembers(String roomId) {
        List<RoomMember> members = editRoomManager.listMembers(roomId);
        Map<String, Object> payload = new HashMap<>(4);
        payload.put("members", members);
        CollabMessage message = CollabMessage.of(CollabMessageTypeEnum.MEMBERS.getValue(), roomId, payload);
        broadcastLocal(roomId, message);
    }

    /**
     * 向单个会话发送一条消息
     * <p>
     * 两处需要小心：
     * <ul>
     *   <li>会话可能在我们发送的一瞬间被关闭（用户关页面、网络断开），
     *       这类失败是正常竞态，不是 bug，按 debug 记录即可；</li>
     *   <li>Tomcat 的 sendMessage 是异步的，连续两次发送会让底层进入
     *       TEXT_PARTIAL_WRITING 状态而抛异常，因此这里对同一会话串行发送并吞掉状态异常。</li>
     * </ul>
     */
    private void sendTo(WebSocketSession session, CollabMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                if (!session.isOpen()) {
                    return;
                }
                session.sendMessage(new TextMessage(json));
            }
        } catch (IllegalStateException e) {
            // 会话已关闭或上一次写入尚未完成：属于正常竞态，不打扰日志
            log.debug("collab send skipped, session not writable: {}", e.getMessage());
        } catch (IOException e) {
            log.debug("collab send failed, session = {}: {}", session.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("collab serialize message error, type = {}", message.getType(), e);
        }
    }
}
