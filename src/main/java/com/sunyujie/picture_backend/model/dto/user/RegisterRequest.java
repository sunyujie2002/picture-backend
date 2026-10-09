package com.sunyujie.picture_backend.model.dto.user;

import lombok.Data;

@Data
public class RegisterRequest {
    private String userAccount;
    private String userPassword;
    private String confirmPassword;
}
