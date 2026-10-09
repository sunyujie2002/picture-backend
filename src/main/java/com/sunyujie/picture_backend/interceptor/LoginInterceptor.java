package com.sunyujie.picture_backend.interceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.sunyujie.picture_backend.constant.SaTokenConstant;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.mapper.UserMapper;
import com.sunyujie.picture_backend.model.dto.user.UserDTO;
import com.sunyujie.picture_backend.model.entity.User;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;

/**
 * 登录态全局拦截器（基于 Sa-Token）
 * <p>
 * checkLogin 未通过时抛 NotLoginException，由全局异常处理器转成 NOT_LOGIN_ERROR；
 * 脱敏用户信息缓存在账户会话里，读不到时回源数据库并补缓存。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. sa-token 校验登录态（token 无效 / 过期抛 NotLoginException）
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        // 2. 从账户会话取脱敏用户信息
        SaSession session = StpUtil.getSession();
        UserDTO sessionUser = (UserDTO) session.get(SaTokenConstant.LOGIN_USER);
        if (sessionUser == null) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在，请重新登录");
            }
            sessionUser = BeanUtil.copyProperties(user, UserDTO.class);
            session.set(SaTokenConstant.LOGIN_USER, sessionUser);
        }
        // 每次请求复制一份再设当前 token：账户会话跨设备共享，不能直接改共享对象
        UserDTO currentUser = BeanUtil.copyProperties(sessionUser, UserDTO.class);
        currentUser.setToken(StpUtil.getTokenValue());
        // 3. 保存到当前线程
        UserContext.setLoginUser(currentUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        // 请求结束必须清理，避免线程池复用导致用户信息串号
        UserContext.clear();
    }
}
