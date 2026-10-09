package com.sunyujie.picture_backend.common;

import java.io.Serializable;

import lombok.Data;
/**
 * 分页类
 */
@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 页码
     */
    private int pageNum;

    /**
     * 每页数量
     */
    private int pageSize;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式
     */
    private String sortOrder;

}
