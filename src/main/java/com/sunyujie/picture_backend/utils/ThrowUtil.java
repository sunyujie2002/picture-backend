package com.sunyujie.picture_backend.utils;

import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;

/**
 * 异常工具类
 */
public class ThrowUtil {
    /**
     * 抛出业务异常
     * @param condition 条件
     * @param errorCode 业务异常
     */
    public static void throwIf(Boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 抛出业务异常
     * @param condition 条件
     * @param errorCode 业务异常
     * @param message 业务异常信息
     */
    public static void throwIf(Boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }
}
