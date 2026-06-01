package com.enterprise_wrapper_api.wrapper_api.security.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private boolean admin = false;
}
