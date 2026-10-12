package com.sunyujie.picture_backend.collab.constant;

/**
 * 图片协作编辑常量
 * <p>
 * Redis key 与消息队列通道命名统一收在这里，避免各处硬编码。
 */
public interface CollabConstant {

    /**
     * WebSocket 握手路径（前端连接地址形如 ws://host/api/ws/picture/edit）
     */
    String WS_ENDPOINT = "/ws/picture/edit";

    /**
     * WebSocket 握手时的图片 id 参数名
     */
    String PARAM_PICTURE_ID = "pictureId";

    /**
     * WebSocket 握手时的 token 参数名
     * <p>
     * 浏览器 WebSocket API 不能在握手时携带自定义请求头，只能用 query 参数传递。
     */
    String PARAM_TOKEN = "token";

    /**
     * Redis Stream 通道：所有房间的编辑消息都进这一个流，
     * 由服务端统一赋全局递增序号后再按 roomId 扇出，从而保证跨实例也有统一顺序。
     */
    String STREAM_KEY = "picture:edit:stream";

    /**
     * 消费组
     */
    String CONSUMER_GROUP = "picture-edit-group";

    /**
     * 房间操作日志 key 前缀（List 结构），完整 key = 前缀 + pictureId
     */
    String ROOM_STATE_KEY = "picture:edit:state:";

    /**
     * 房间元信息（secuencia 计数器）key 前缀，完整 key = 前缀 + pictureId
     */
    String ROOM_META_KEY = "picture:edit:meta:";

    /**
     * 操作日志保留时长（秒）：30 分钟，属于"未保存的草稿"，
     * 超时后房间回到干净状态，下次打开从原图开始，避免脏草稿长期污染。
     */
    long ROOM_STATE_TTL_SECONDS = 30 * 60;

    /**
     * 单个房间最多缓存的操作条数，超出后裁剪最旧的操作（前端已足够收敛）
     */
    int MAX_OPS_PER_ROOM = 500;

    /**
     * 单实例心跳间隔兜底（毫秒），用于清理假在线成员
     */
    long HEARTBEAT_TIMEOUT_MS = 60 * 1000L;

    /**
     * WebSocket 单条消息最大字节数（画布对象序列化后可能较大）
     */
    int MAX_TEXT_MESSAGE_SIZE = 8 * 1024 * 1024;
}
