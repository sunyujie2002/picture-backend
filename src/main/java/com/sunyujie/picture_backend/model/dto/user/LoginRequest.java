package com.sunyujie.picture_backend.model.dto.user;

import lombok.Data;

@Data
public class LoginRequest {
    private String userAccount;
    private String userPassword;
}
