package com.sunyujie.picture_backend.utils;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.exception.ErrorCode;

public class ResultUtil {
    /**
     * 成功
     * @param data 数据
     * @return 结果
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功
     * @param data 数据
     * @param message 信息
     * @return 结果
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败
     * @param code 状态码
     * @param message 信息
     * @return 结果
     */
    public static <T> BaseResponse<T> fail(int code, String message) {
        return new BaseResponse<>(code, message, null);
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @return 结果
     */
    public static <T> BaseResponse<T> fail(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @param message 信息
     * @return 结果
     */
    public static <T> BaseResponse<T> fail(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode, message);
    }
}