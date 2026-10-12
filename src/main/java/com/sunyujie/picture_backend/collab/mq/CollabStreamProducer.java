package com.sunyujie.picture_backend.collab.mq;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunyujie.picture_backend.collab.constant.CollabConstant;
import com.sunyujie.picture_backend.collab.model.CollabMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * 协作消息生产者：把编辑消息投递到 Redis Stream
 * <p>
 * 为什么所有房间共用一条 Stream：
 * <ul>
 *   <li>服务端要保证「同一房间内所有操作的顺序一致」，单一通道天然全局有序；</li>
 *   <li>多实例部署时每个实例作为消费组内的一员，横向扩展无需额外协调组件；</li>
 *   <li>Stream 有持久化与消费位置管理（pending / ack），比 Pub/Sub 更适合当消息队列。</li>
 * </ul>
 */
@Slf4j
@Component
public class CollabStreamProducer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 投递一条协作消息
     *
     * @return Stream 消息 id；失败返回 null
     */
    public String send(CollabMessage message) {
        try {
            Map<String, String> body = new HashMap<>(8);
            body.put("type", message.getType());
            body.put("roomId", message.getRoomId());
            body.put("userId", message.getUserId() == null ? "" : message.getUserId());
            body.put("timestamp", String.valueOf(
                    message.getTimestamp() == null ? System.currentTimeMillis() : message.getTimestamp()));
            body.put("payload", objectMapper.writeValueAsString(message.getPayload()));

            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .ofMap(body)
                    .withStreamKey(CollabConstant.STREAM_KEY);
            RecordId recordId = stringRedisTemplate.opsForStream().add(record);
            return recordId == null ? null : recordId.getValue();
        } catch (Exception e) {
            log.error("collab stream send error, type = {}, roomId = {}", message.getType(), message.getRoomId(), e);
            return null;
        }
    }
}
