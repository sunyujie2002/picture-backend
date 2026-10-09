package com.sunyujie.picture_backend.interceptor;

import java.util.Arrays;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;

@Aspect 
@Component
public class AuthInterceptor {
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        if (mustRole.isEmpty()) {
            mustRole = UserRoleEnum.ADMIN.getValue();
        }
        List<String> roleList = Arrays.asList(mustRole.split(","));
        String userRole = UserContext.getLoginUser().getUserRole();
        if (!roleList.contains(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
