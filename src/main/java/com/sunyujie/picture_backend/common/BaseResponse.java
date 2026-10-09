package com.sunyujie.picture_backend.common;

import com.sunyujie.picture_backend.exception.ErrorCode;

import lombok.Data;

/**
 * 结果类
 * 
 * @param <T> 数据类型
 */
@Data
public class BaseResponse<T> {
    /**
     * 状态码
     */
    private int code;
    /**
     * 信息
     */
    private String message;
    /**
     * 数据
     */
    private T data;

    /**
     * 构造方法
     */
    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public BaseResponse(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, null);
    }
}
