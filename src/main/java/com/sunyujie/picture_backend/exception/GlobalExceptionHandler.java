package com.sunyujie.picture_backend.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.utils.ResultUtil;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handleBusinessException(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtil.fail(e.getCode(), e.getMessage());
    }

    /**
     * Sa-Token 未登录（token 无效 / 过期 / 被顶下线）
     */
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> handleNotLoginException(NotLoginException e) {
        log.warn("NotLoginException: {}", e.getMessage());
        return ResultUtil.fail(ErrorCode.NOT_LOGIN_ERROR, "登录已过期，请重新登录");
    }

    /**
     * Sa-Token 权限 / 角色不足
     */
    @ExceptionHandler({ NotPermissionException.class, NotRoleException.class })
    public BaseResponse<?> handleNotPermissionException(RuntimeException e) {
        log.warn("AuthDeniedException: {}", e.getMessage());
        return ResultUtil.fail(ErrorCode.NO_AUTH_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception e) {
        log.error("RuntimeException", e);
        return ResultUtil.fail(ErrorCode.SYSTEM_ERROR);
    }
}
