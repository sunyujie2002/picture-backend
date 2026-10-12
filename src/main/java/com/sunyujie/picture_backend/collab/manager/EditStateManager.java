package com.sunyujie.picture_backend.collab.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.sunyujie.picture_backend.collab.constant.CollabConstant;
import com.sunyujie.picture_backend.collab.model.EditOperation;
import com.sunyujie.picture_backend.collab.model.EditRoomState;

import lombok.extern.slf4j.Slf4j;

/**
 * 房间编辑状态（草稿层）持久化
 * <p>
 * 数据结构刻意选择 Redis List + RPUSH 追加：
 * 多人的操作可能并发到达，如果采用「读出整体 → 追加 → 写回」会有读改写竞争导致丢操作，
 * 而 RPUSH 是原子操作，天然安全。
 * <p>
 * 语义约定（很重要）：
 * <ul>
 *   <li>这里存的是<b>尚未固化到图片</b>的操作，属于临时草稿，带 30 分钟过期；</li>
 *   <li>只有图片被真正覆盖保存后才调用 {@link #clearRoom(String)} 清空草稿；</li>
 *   <li>因此「改了但没点保存就关闭」重开页面时依然是原图，草稿不会污染图片本身。</li>
 * </ul>
 */
@Slf4j
@Component
public class EditStateManager {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 读取房间草稿状态；无草稿时返回 version = 0 的空状态
     */
    public EditRoomState getRoomState(String roomId) {
        String opsKey = opsKey(roomId);
        Long size = redisTemplate.opsForList().size(opsKey);
        List<Object> raw = size != null && size > 0
                ? redisTemplate.opsForList().range(opsKey, 0, -1)
                : new ArrayList<>();

        List<EditOperation> operations = new ArrayList<>();
        if (raw != null) {
            for (Object item : raw) {
                if (item instanceof EditOperation) {
                    operations.add((EditOperation) item);
                }
            }
        }
        EditRoomState state = new EditRoomState();
        state.setRoomId(roomId);
        state.setOperations(operations);
        state.setVersion(operations.isEmpty() ? 0L : operations.get(operations.size() - 1).getSeq());
        state.setUpdatedAt(System.currentTimeMillis());
        return state;
    }

    /**
     * 分配房间内递增序号
     * <p>
     * 走 Redis 自增而非本地计数，保证多实例部署时序号依然单调；
     * 这个序号是所有客户端按同一顺序重放操作的唯一依据。
     */
    public Long nextSeq(String roomId) {
        Long seq = redisTemplate.opsForValue().increment(seqKey(roomId), 1);
        redisTemplate.expire(seqKey(roomId), CollabConstant.ROOM_STATE_TTL_SECONDS, TimeUnit.SECONDS);
        return seq == null ? System.currentTimeMillis() : seq;
    }

    /**
     * 原子追加一条已定序的操作；超出上限时裁剪最旧的
     */
    public void appendOperation(String roomId, EditOperation operation) {
        String opsKey = opsKey(roomId);
        redisTemplate.opsForList().rightPush(opsKey, operation);
        redisTemplate.expire(opsKey, CollabConstant.ROOM_STATE_TTL_SECONDS, TimeUnit.SECONDS);

        Long size = redisTemplate.opsForList().size(opsKey);
        if (size != null && size > CollabConstant.MAX_OPS_PER_ROOM) {
            // 保留最新的 MAX_OPS_PER_ROOM 条：左侧多余的直接裁掉
            redisTemplate.opsForList().trim(opsKey, size - CollabConstant.MAX_OPS_PER_ROOM, -1);
        }
    }

    /**
     * 操作是否已存在（按客户端 opId 去重，重连/重试时同一条可能重复到达）
     */
    public boolean opExists(String roomId, String opId) {
        if (opId == null) {
            return false;
        }
        List<Object> raw = redisTemplate.opsForList().range(opsKey(roomId), 0, -1);
        if (raw == null) {
            return false;
        }
        for (Object item : raw) {
            if (item instanceof EditOperation && opId.equals(((EditOperation) item).getOpId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空房间草稿
     * <p>
     * 两个调用时机：
     * <ol>
     *   <li>有成员保存成功、图片已被覆盖 —— 草稿已固化到图片；</li>
     *   <li>最后一个成员离开 —— 无人编辑，丢弃草稿。</li>
     * </ol>
     */
    public void clearRoom(String roomId) {
        redisTemplate.delete(opsKey(roomId));
        redisTemplate.delete(seqKey(roomId));
        log.info("collab room draft cleared, roomId = {}", roomId);
    }

    private String opsKey(String roomId) {
        return CollabConstant.ROOM_STATE_KEY + roomId;
    }

    private String seqKey(String roomId) {
        return CollabConstant.ROOM_META_KEY + roomId + ":seq";
    }
}
