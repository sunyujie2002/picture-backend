package com.sunyujie.picture_backend.collab.config;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.sunyujie.picture_backend.collab.constant.CollabConstant;
import com.sunyujie.picture_backend.collab.manager.EditAuthManager;
import com.sunyujie.picture_backend.collab.model.dto.EditAuthResult;
import com.sunyujie.picture_backend.collab.websocket.PictureEditHandler;
import com.sunyujie.picture_backend.mapper.UserMapper;
import com.sunyujie.picture_backend.model.entity.User;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 配置：注册协作端点并完成握手阶段鉴权
 * <p>
 * 握手必须在 beforeHandshake 阶段完成校验 —— 一旦握手成功再鉴权，
 * 未授权用户已经连进来占用了房间席位，且后续每个消息都要重新校验。
 */
@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private EditAuthManager editAuthManager;

    @Resource
    private UserMapper userMapper;

    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandler, CollabConstant.WS_ENDPOINT)
                .addInterceptors(new CollabHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    /**
     * 握手拦截器：校验 token 与图片编辑权限，并把身份信息写进 session 属性
     */
    private class CollabHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            HttpServletRequest servletRequest =
                    request instanceof ServletServerHttpRequest
                            ? ((ServletServerHttpRequest) request).getServletRequest()
                            : null;
            if (servletRequest == null) {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }
            String token = servletRequest.getParameter(CollabConstant.PARAM_TOKEN);
            String pictureId = servletRequest.getParameter(CollabConstant.PARAM_PICTURE_ID);
            if (token == null || token.isEmpty() || pictureId == null || pictureId.isEmpty()) {
                log.warn("collab handshake missing params, pictureId = {}", pictureId);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 1. 校验 token：浏览器 WebSocket 无法携带自定义请求头，只能走 query 参数
            Long userId;
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                userId = Long.valueOf(String.valueOf(loginId));
            } catch (Exception e) {
                log.warn("collab handshake invalid token, pictureId = {}", pictureId);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            User user = userMapper.selectById(userId);
            if (user == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 2. 校验图片编辑权限（公共图本人 / 个人空间创建者 / 团队空间 editor|admin）
            EditAuthResult authResult;
            try {
                authResult = editAuthManager.authPictureEdit(Long.valueOf(pictureId), userId, user.getUserRole());
            } catch (Exception e) {
                log.warn("collab handshake auth failed, userId = {}, pictureId = {}, reason = {}",
                        userId, pictureId, e.getMessage());
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put("roomId", pictureId);
            attributes.put("userId", String.valueOf(userId));
            attributes.put("userName", user.getUserName());
            attributes.put("spaceRole", authResult.getSpaceRole());
            attributes.put("spaceId", authResult.getSpaceId());
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
            // 无需处理
        }
    }
}
