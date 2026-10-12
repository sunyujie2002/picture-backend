package com.sunyujie.picture_backend.collab.mq;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunyujie.picture_backend.collab.constant.CollabConstant;
import com.sunyujie.picture_backend.collab.model.CollabMessage;
import com.sunyujie.picture_backend.collab.model.EditOperation;
import com.sunyujie.picture_backend.collab.model.EditRoomState;
import com.sunyujie.picture_backend.collab.model.enums.CollabMessageTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * 协作消息消费者：把 Redis Stream 里的编辑消息取出并交给业务处理器
 * <p>
 * 关于消费组的取法，这里有一个容易踩的坑值得说明：
 * <b>同一消费组内，一条消息只会投递给其中一个消费者</b>。如果多个后端实例共用同一个组名，
 * 那么每条操作只会进入其中一个实例，连接在其他实例上的成员就永远看不到这次编辑。
 * <p>
 * 因此我们让<b>每个实例使用独立的消费组</b>（组名带实例标识）：
 * <ul>
 *   <li>每个实例都能拿到全量消息 → 实现跨实例扇出；</li>
 *   <li>组内只有一个消费者 → 同一条消息在本实例不会被重复处理；</li>
 *   <li>依然具备队列语义：持久化、消费位点、pending/ack 一应俱全。</li>
 * </ul>
 * 实例关闭时销毁自己的消费组，避免无用组长期堆积在 Redis 里。
 */
@Slf4j
@Component
public class CollabStreamConsumer {

    /**
     * 轮询间隔：取空单个实例足够轻量，避免引入监听器容器的序列化不确定性
     */
    private static final long POLL_INTERVAL_MS = 200L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final CollabStreamListener listener;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String instanceId = java.util.UUID.randomUUID().toString().substring(0, 8);

    private final String consumerName = "collab-" + instanceId;

    private final String consumerGroup = CollabConstant.CONSUMER_GROUP + "-" + instanceId;

    private ScheduledExecutorService scheduler;

    public CollabStreamConsumer(CollabStreamListener listener) {
        this.listener = listener;
    }

    @PostConstruct
    public void init() {
        try {
            prepareConsumerGroup();
        } catch (Exception e) {
            log.warn("collab consumer group prepare failed: {}", e.getMessage());
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "collab-stream-consumer");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::pollOnce, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        log.info("collab stream consumer started, stream = {}, group = {}", CollabConstant.STREAM_KEY, consumerGroup);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        try {
            stringRedisTemplate.opsForStream().destroyGroup(CollabConstant.STREAM_KEY, consumerGroup);
        } catch (Exception e) {
            // 停服时 Redis 可能已不可用，忽略即可
            log.debug("collab consumer group destroy skipped: {}", e.getMessage());
        }
    }

    /**
     * 保证 Stream 与消费组存在
     * <p>
     * 消费组不能建立在尚不存在的 Stream 上，所以先写入一条初始化消息占位，建组后再删除。
     */
    private void prepareConsumerGroup() {
        MapRecord<String, String, String> initRecord = StreamRecords.newRecord()
                .ofMap(Collections.singletonMap("init", "1"))
                .withStreamKey(CollabConstant.STREAM_KEY);
        RecordId initId = stringRedisTemplate.opsForStream().add(initRecord);
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(CollabConstant.STREAM_KEY, ReadOffset.latest(), consumerGroup);
            log.info("collab consumer group created: {}", consumerGroup);
        } catch (Exception e) {
            log.info("collab consumer group already exists, skipped: {}", e.getMessage());
        }
        if (initId != null) {
            stringRedisTemplate.opsForStream().delete(CollabConstant.STREAM_KEY, initId);
        }
    }

    /**
     * 拉取一批新消息并处理
     */
    private void pollOnce() {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                    .read(Consumer.from(consumerGroup, consumerName),
                            StreamOffset.create(CollabConstant.STREAM_KEY, ReadOffset.lastConsumed()));
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                handleRecord(record);
            }
        } catch (Exception e) {
            log.warn("collab stream poll error: {}", e.getMessage());
        }
    }

    /**
     * 处理单条记录：反序列化 → 业务处理 → ack
     */
    private void handleRecord(MapRecord<String, Object, Object> record) {
        String type = null;
        String roomId = null;
        try {
            Map<String, String> body = toStringMap(record.getValue());
            type = body.get("type");
            roomId = body.get("roomId");
            String rawPayload = body.get("payload");
            Object payload = null;
            if (rawPayload != null && !rawPayload.isEmpty() && !"null".equals(rawPayload)) {
                // 按类型还原成具体结构，而不是统一读成 Object：
                // 否则同一条操作在快照里是 EditOperation（字段类型完整），
                // 经队列广播后变成 Map（数字/字符串被抹成通用类型），两端字段形态不一致。
                if (CollabMessageTypeEnum.OP_BROADCAST.getValue().equals(type)) {
                    payload = objectMapper.readValue(rawPayload, EditOperation.class);
                } else if (CollabMessageTypeEnum.SNAPSHOT.getValue().equals(type)) {
                    payload = objectMapper.readValue(rawPayload, EditRoomState.class);
                } else {
                    payload = objectMapper.readValue(rawPayload, Object.class);
                }
            }
            CollabMessage message = new CollabMessage();
            message.setType(type);
            message.setRoomId(roomId);
            message.setUserId(body.get("userId"));
            message.setPayload(payload);
            String timestamp = body.get("timestamp");
            message.setTimestamp(timestamp == null ? System.currentTimeMillis() : Long.parseLong(timestamp));

            listener.onCollabMessage(message);
        } catch (Exception e) {
            log.error("collab stream message handle error, type = {}, roomId = {}", type, roomId, e);
        } finally {
            ack(record);
        }
    }

    private void ack(MapRecord<String, Object, Object> record) {
        try {
            stringRedisTemplate.opsForStream()
                    .acknowledge(CollabConstant.STREAM_KEY, consumerGroup, record.getId());
        } catch (Exception e) {
            log.warn("collab stream ack failed: {}", e.getMessage());
        }
    }

    /**
     * StringRedisTemplate 的 hash 值泛型是 Object，但实际序列化器是字符串，这里统一转成字符串 map
     */
    private Map<String, String> toStringMap(Map<Object, Object> raw) {
        if (raw == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new java.util.HashMap<>(raw.size() * 2);
        raw.forEach((key, value) -> result.put(String.valueOf(key), value == null ? null : String.valueOf(value)));
        return result;
    }
}
