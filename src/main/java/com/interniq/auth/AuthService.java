package com.interniq.auth;

import com.interniq.audit.AuditLogService;
import com.interniq.audit.LoginAuditService;
import com.interniq.audit.LoginAuditStatus;
import com.interniq.auth.dto.AuthSession;
import com.interniq.auth.dto.ChangePasswordRequest;
import com.interniq.auth.dto.ForgotPasswordRequest;
import com.interniq.auth.dto.LoginRequest;
import com.interniq.auth.dto.LoginResponse;
import com.interniq.auth.dto.RefreshTokenResponse;
import com.interniq.auth.dto.RegisterRequest;
import com.interniq.auth.dto.ResetPasswordRequest;
import com.interniq.exception.BadRequestException;
import com.interniq.exception.UnauthorizedException;
import com.interniq.password.PasswordResetToken;
import com.interniq.password.PasswordResetService;
import com.interniq.security.JwtService;
import com.interniq.token.RefreshToken;
import com.interniq.token.RefreshTokenService;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final String SAFE_RESET_MESSAGE = "If this email exists, reset instructions have been sent.";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAuditService loginAuditService;
    private final PasswordResetService passwordResetService;
    private final AuditLogService auditLogService;

    @Transactional
    public LoginResponse.UserInfo register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        auditLogService.record(savedUser, "USER_REGISTERED", "User", savedUser.getId());
        return userService.toUserInfo(savedUser);
    }

    @Transactional
    public AuthSession login(LoginRequest request, HttpServletRequest servletRequest) {
        String email = normalizeEmail(request.getEmail());
        String ipAddress = ipAddress(servletRequest);
        String userAgent = userAgent(servletRequest);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            loginAuditService.record(null, email, LoginAuditStatus.FAILED, ipAddress, userAgent, "User not found");
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            loginAuditService.record(user, email, LoginAuditStatus.FAILED, ipAddress, userAgent, "Account inactive");
            throw new AccessDeniedException("Account is inactive");
        }

        if (user.isAccountLocked()) {
            loginAuditService.record(user, email, LoginAuditStatus.LOCKED, ipAddress, userAgent, "Account locked");
            throw new AccessDeniedException("Account is locked. Please contact an administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            LoginAuditStatus status = LoginAuditStatus.FAILED;
            String reason = "Invalid password";
            if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setStatus("LOCKED");
                status = LoginAuditStatus.LOCKED;
                reason = "Account locked after failed login attempts";
            }
            loginAuditService.record(user, email, status, ipAddress, userAgent, reason);
            throw new BadCredentialsException("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("ACTIVE");
        }

        RefreshToken refreshToken = refreshTokenService.create(user, userAgent, ipAddress);
        LoginResponse response = loginResponse(user);
        auditLogService.record(user, "USER_LOGIN", "User", user.getId());
        loginAuditService.record(user, email, LoginAuditStatus.SUCCESS, ipAddress, userAgent, null);

        return AuthSession.builder()
                .loginResponse(response)
                .refreshToken(refreshToken.getToken())
                .refreshExpiresInMs(refreshTokenService.getRefreshExpirationMs())
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getCurrentUser(Authentication authentication) {
        return userService.toUserInfo(userService.getCurrentUser(authentication));
    }

    @Transactional
    public AuthSession refresh(String refreshTokenValue, HttpServletRequest servletRequest) {
        RefreshToken newRefreshToken = refreshTokenService.rotate(refreshTokenValue, userAgent(servletRequest), ipAddress(servletRequest));
        User user = newRefreshToken.getUser();

        if (!user.isActive() || user.isAccountLocked()) {
            throw new UnauthorizedException("User account is not allowed to refresh session");
        }

        loginAuditService.record(user, user.getEmail(), LoginAuditStatus.REFRESH, ipAddress(servletRequest), userAgent(servletRequest), null);

        return AuthSession.builder()
                .refreshResponse(refreshResponse(user))
                .refreshToken(newRefreshToken.getToken())
                .refreshExpiresInMs(refreshTokenService.getRefreshExpirationMs())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue, Authentication authentication, HttpServletRequest request) {
        User user = currentUserOrNull(authentication);
        refreshTokenService.revokeByToken(refreshTokenValue);
        loginAuditService.record(user, user == null ? "unknown" : user.getEmail(), LoginAuditStatus.LOGOUT, ipAddress(request), userAgent(request), null);
    }

    @Transactional
    public void logoutAll(Authentication authentication, HttpServletRequest request) {
        User user = userService.getCurrentUser(authentication);
        refreshTokenService.revokeAll(user);
        loginAuditService.record(user, user.getEmail(), LoginAuditStatus.LOGOUT, ipAddress(request), userAgent(request), "Logout all sessions");
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        String email = normalizeEmail(request.getEmail());
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            passwordResetService.create(user);
            loginAuditService.record(user, email, LoginAuditStatus.PASSWORD_RESET_REQUEST, ipAddress(servletRequest), userAgent(servletRequest), null);
        });
        return SAFE_RESET_MESSAGE;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest servletRequest) {
        PasswordResetToken resetToken = passwordResetService.use(request.getToken());
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setStatus("ACTIVE");
        refreshTokenService.revokeAll(user);
        loginAuditService.record(user, user.getEmail(), LoginAuditStatus.PASSWORD_RESET_SUCCESS, ipAddress(servletRequest), userAgent(servletRequest), null);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, Authentication authentication, HttpServletRequest servletRequest) {
        User user = userService.getCurrentUser(authentication);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenService.revokeAll(user);
        loginAuditService.record(user, user.getEmail(), LoginAuditStatus.PASSWORD_CHANGE, ipAddress(servletRequest), userAgent(servletRequest), null);
    }

    private LoginResponse loginResponse(User user) {
        String token = jwtService.generateToken(user);
        return LoginResponse.builder()
                .accessToken(token)
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(userService.toUserInfo(user))
                .build();
    }

    private RefreshTokenResponse refreshResponse(User user) {
        String token = jwtService.generateToken(user);
        return RefreshTokenResponse.builder()
                .accessToken(token)
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .build();
    }

    private User currentUserOrNull(Authentication authentication) {
        try {
            return userService.getCurrentUser(authentication);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String ipAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
