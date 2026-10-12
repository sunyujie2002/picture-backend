package com.sunyujie.picture_backend.common;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import lombok.Data;

/**
 * 批量更新排序请求（分类 / 标签共用）
 */
@Data
public class SortBatchRequest {

    /**
     * 需要更新排序的条目；只传顺序发生变化的条目即可
     */
    @NotEmpty(message = "排序内容不能为空")
    @Valid
    private List<SortItem> items;
}
