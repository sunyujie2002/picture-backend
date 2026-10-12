package com.sunyujie.picture_backend.exception;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.utils.ResultUtil;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.util.StrUtil;
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
     * Sa-Token 未登录统一入口，按异常类型细分提示文案（错误码都是 NOT_LOGIN_ERROR，前端一律跳登录页）：
     * NOT_TOKEN      请求头根本没带 token        → 未登录
     * INVALID_TOKEN  带了 token，但查无此登录态   → 登录状态无效（伪造/服务端已清）
     * TOKEN_TIMEOUT  token 超过有效期（24h）      → 登录已过期
     * BE_REPLACED    同账号在别处登录，被顶下线    → 被挤下线
     * KICK_OUT       被服务端主动踢下线（如改密）  → 已被强制下线
     */
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> handleNotLoginException(NotLoginException e) {
        String message;
        switch (e.getType()) {
            case NotLoginException.INVALID_TOKEN:
                message = "登录状态无效，请重新登录";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = "登录已过期，请重新登录";
                break;
            case NotLoginException.BE_REPLACED:
                message = "账号已在其他设备登录，你已被迫下线";
                break;
            case NotLoginException.KICK_OUT:
                message = "账号已被强制下线，请重新登录";
                break;
            case NotLoginException.NOT_TOKEN:
            default:
                message = "未登录，请先登录";
                break;
        }
        log.warn("NotLoginException, type = {}, message = {}", e.getType(), e.getMessage());
        return ResultUtil.fail(ErrorCode.NOT_LOGIN_ERROR, message);
    }

    /**
     * Sa-Token 权限 / 角色不足
     */
    @ExceptionHandler({ NotPermissionException.class, NotRoleException.class })
    public BaseResponse<?> handleNotPermissionException(RuntimeException e) {
        log.warn("AuthDeniedException: {}", e.getMessage());
        return ResultUtil.fail(ErrorCode.NO_AUTH_ERROR);
    }

    /**
     * @Valid / @Validated 参数校验失败
     * <p>
     * 不接管的话会落到下面的通用 Exception 分支，前端收到「系统内部异常」，
     * 用户根本不知道自己填错了什么。这里取第一条字段错误原文返回，
     * 文案本身已与前端表单校验保持一致。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtil.fail(ErrorCode.PARAMS_ERROR, message);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception e) {
        log.error("RuntimeException", e);
        return ResultUtil.fail(ErrorCode.SYSTEM_ERROR);
    }
}
