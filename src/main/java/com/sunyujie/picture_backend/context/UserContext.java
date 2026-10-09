package com.sunyujie.picture_backend.context;

import com.sunyujie.picture_backend.model.dto.user.UserDTO;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 * 拦截器校验 token 通过后把用户信息存进来，请求结束后清除
 */
public class UserContext {

    private static final ThreadLocal<UserDTO> LOGIN_USER_HOLDER = new ThreadLocal<>();

    public static void setLoginUser(UserDTO loginUser) {
        LOGIN_USER_HOLDER.set(loginUser);
    }

    public static UserDTO getLoginUser() {
        return LOGIN_USER_HOLDER.get();
    }

    public static Long getLoginUserId() {
        UserDTO loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getId();
    }

    public static String getToken() {
        return getLoginUser().getToken();
    }

    public static String getRole() {
        return getLoginUser().getUserRole();
    }

    /**
     * 清除登录用户上下文
     */
    public static void clear() {
        LOGIN_USER_HOLDER.remove();
    }
}
