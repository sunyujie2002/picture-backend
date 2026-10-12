package com.sunyujie.picture_backend.collab.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房间编辑状态（"草稿层"）
 * <p>
 * 这里保存的是尚未固化到图片的编辑操作。只有调用图片更新接口把新图写入 COS 之后，
 * 房间才会被清空；在此之前，任何一个成员没点保存就关闭页面，重开时依然是原图 —— 草稿不落库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditRoomState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房间标识（图片 id）
     */
    private String roomId;

    /**
     * 当前已应用到的最新序号（跟随全局约定，JSON 中为字符串形态）
     */
    private Long version;

    /**
     * 操作日志（按 seq 升序）
     */
    private List<EditOperation> operations = new ArrayList<>();

    /**
     * 房间最后活动时间
     */
    private Long updatedAt;
}
