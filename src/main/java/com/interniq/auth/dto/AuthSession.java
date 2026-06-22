package com.interniq.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSession {
    private LoginResponse loginResponse;
    private RefreshTokenResponse refreshResponse;
    private String refreshToken;
    private long refreshExpiresInMs;
}
