package com.sunyujie.picture_backend.collab.mq;

import com.sunyujie.picture_backend.collab.model.CollabMessage;

/**
 * Stream 消息回调：由 WebSocket 主处理器实现，负责把队列里的消息广播给本地会话
 */
public interface CollabStreamListener {

    void onCollabMessage(CollabMessage message);
}
