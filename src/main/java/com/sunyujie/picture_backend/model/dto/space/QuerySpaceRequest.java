package com.sunyujie.picture_backend.model.dto.space;

import com.sunyujie.picture_backend.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询空间请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuerySpaceRequest extends PageRequest {
    /**
     * 空间类型
     */
    private Integer spaceType;

    /**
     * 空间级别
     */
    private Integer spaceLevel;

    /**
     * 空间名称
     */
    private String spaceName;
}
