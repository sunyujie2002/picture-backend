package com.sunyujie.picture_backend.collab.model;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次画布操作
 * <p>
 * 设计要点：
 * <ul>
 *   <li>opId 由客户端生成并携带，服务端据此去重，避免重试/重连导致重复应用；</li>
 *   <li>seq 由服务端统一分配（房间内递增），所有端按同一 seq 顺序重放，状态必然收敛；</li>
 *   <li>targetId 是画布元素的全局 id，是「对象级并发」的依据：</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作 id（客户端生成 UUID，用于幂等去重）
     */
    private String opId;

    /**
     * 服务端分配的房间内递增序号，定序的唯一依据。
     *
     * 注意：跟随项目全局约定（{@code JsonConfig} 把 Long 统一序列化为字符串以防雪花 ID 丢精度），
     * 这个字段在 JSON 里同样是字符串形态。客户端按"服务端下发的顺序"应用操作即可，
     * 不要拿它做数值比较（字符串序下 "10" 会小于 "9"）。
     */
    private Long seq;

    /**
     * 操作发起者用户 id（字符串，保持 Long→String 的精度约定）
     */
    private String userId;

    /**
     * 操作发起者用户名，用于界面提示（避免前端再查一次）
     */
    private String userName;

    /**
     * 操作类型，取值见 EditActionEnum
     */
    private String action;

    /**
     * 画布元素 id；UNDO 时指向被撤销操作的 opId
     */
    private String targetId;

    /**
     * 元素载荷：新增/更新时为序列化后的元素描述（fabric.toObject 的等价结构），
     * 删除/撤销时为空
     */
    private Map<String, Object> payload;

    /**
     * 操作发生时间
     */
    private Long timestamp;
}
