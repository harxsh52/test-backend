package com.interniq.auth;

import com.interniq.auth.dto.AuthSession;
import com.interniq.auth.dto.ChangePasswordRequest;
import com.interniq.auth.dto.ForgotPasswordRequest;
import com.interniq.auth.dto.LoginRequest;
import com.interniq.auth.dto.LoginResponse;
import com.interniq.auth.dto.RefreshTokenResponse;
import com.interniq.auth.dto.RegisterRequest;
import com.interniq.auth.dto.ResetPasswordRequest;
import com.interniq.common.ApiResponse;
import com.interniq.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    @Value("${application.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${application.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${application.security.cookie.path:/api/auth}")
    private String cookiePath;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse.UserInfo user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthSession session = authService.login(request, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(ApiResponse.success("Login successful", session.getLoginResponse()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletRequest servletRequest
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token cookie is missing");
        }
        AuthSession session = authService.refresh(refreshToken, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(ApiResponse.success("Token refreshed successfully", session.getRefreshResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        authService.logout(refreshToken, authentication, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication authentication, HttpServletRequest servletRequest) {
        authService.logoutAll(authentication, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Logged out from all sessions successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getCurrentUser(Authentication authentication) {
        LoginResponse.UserInfo user = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(ApiResponse.success("Current user loaded successfully", user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        authService.changePassword(request, authentication, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Password changed successfully. Please login again.", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPassword(request, servletRequest), null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest servletRequest) {
        authService.resetPassword(request, servletRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Password reset successfully. Please login again.", null));
    }

    private ResponseCookie refreshCookie(AuthSession session) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, session.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(session.getRefreshExpiresInMs() / 1000)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(0)
                .build();
    }
}
