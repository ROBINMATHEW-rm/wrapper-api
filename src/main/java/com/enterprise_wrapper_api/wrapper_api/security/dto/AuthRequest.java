package com.enterprise_wrapper_api.wrapper_api.security.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
