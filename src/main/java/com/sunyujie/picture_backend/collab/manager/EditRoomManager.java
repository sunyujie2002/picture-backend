package com.sunyujie.picture_backend.collab.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.sunyujie.picture_backend.collab.model.RoomMember;

import lombok.extern.slf4j.Slf4j;

/**
 * 协作房间：维护本实例内「房间 → WebSocket 会话 / 成员」的映射
 * <p>
 * 会话只存在于单个 JVM，跨实例的场景由 Redis Stream 扇出：
 * 本实例收到自己成员的会话后往 MQ 投递消息，再由各实例消费者广播给自己本地的会话。
 */
@Slf4j
@Component
public class EditRoomManager {

    /**
     * 协作头像标识色：相邻成员颜色差异明显，够用例里的多人在线区分即可
     */
    private static final String[] MEMBER_COLORS = {
            "#1677ff", "#52c41a", "#fa8c16", "#eb2f96",
            "#722ed1", "#13c2c2", "#f5222d", "#a0d911"
    };

    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    private final Map<String, Map<String, RoomMember>> roomMembers = new ConcurrentHashMap<>();

    /**
     * 加入房间，返回加入后的成员列表快照
     */
    public List<RoomMember> join(String roomId, WebSocketSession session, RoomMember member) {
        rooms.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>()).put(session.getId(), session);
        roomMembers.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>()).put(session.getId(), member);
        log.info("collab member joined, roomId = {}, userId = {}, session = {}", roomId, member.getUserId(),
                session.getId());
        return listMembers(roomId);
    }

    /**
     * 离开房间
     *
     * @return 房间是否因此变空（调用方据此决定是否清理草稿）
     */
    public boolean leave(String roomId, WebSocketSession session) {
        Map<String, WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                rooms.remove(roomId);
            }
        }
        Map<String, RoomMember> members = roomMembers.get(roomId);
        if (members != null) {
            members.remove(session.getId());
            if (members.isEmpty()) {
                roomMembers.remove(roomId);
                log.info("collab room empty, roomId = {}", roomId);
                return true;
            }
        }
        return false;
    }

    /**
     * 获取房间内的在线成员
     */
    public List<RoomMember> listMembers(String roomId) {
        Map<String, RoomMember> members = roomMembers.get(roomId);
        if (members == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(members.values());
    }

    /**
     * 获取本实例内该房间的全部会话
     */
    public List<WebSocketSession> listSessions(String roomId) {
        Map<String, WebSocketSession> sessions = rooms.get(roomId);
        if (sessions == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(sessions.values());
    }

    /**
     * 房间当前是否还有会话
     */
    public boolean isEmpty(String roomId) {
        Map<String, WebSocketSession> sessions = rooms.get(roomId);
        return sessions == null || sessions.isEmpty();
    }

    /**
     * 按用户 id 稳定散列出一个头像色，同一用户在不同客户端看到同样的颜色
     */
    public String colorFor(String userId) {
        if (userId == null) {
            return MEMBER_COLORS[0];
        }
        int hash = userId.hashCode();
        int index = Math.abs(hash) % MEMBER_COLORS.length;
        return MEMBER_COLORS[index];
    }
}
