package com.interniq.auth;

import com.interniq.audit.AuditLogService;
import com.interniq.audit.LoginAuditService;
import com.interniq.auth.dto.AuthSession;
import com.interniq.auth.dto.LoginRequest;
import com.interniq.auth.dto.LoginResponse;
import com.interniq.auth.dto.RegisterRequest;
import com.interniq.password.PasswordResetService;
import com.interniq.security.JwtService;
import com.interniq.token.RefreshToken;
import com.interniq.token.RefreshTokenService;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private LoginAuditService loginAuditService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUserSuccessfully() {
        RegisterRequest request = registerRequest();
        User savedUser = user(1L, Role.INTERN);

        when(userRepository.existsByEmailIgnoreCase("intern@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userService.toUserInfo(savedUser)).thenReturn(userInfo(savedUser));

        LoginResponse.UserInfo response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("intern@test.com");
        assertThat(response.getRole()).isEqualTo(Role.INTERN);
        verify(userRepository).save(any(User.class));
        verify(auditLogService).record(savedUser, "USER_REGISTERED", "User", 1L);
    }

    @Test
    void preventDuplicateEmailRegistration() {
        RegisterRequest request = registerRequest();

        when(userRepository.existsByEmailIgnoreCase("intern@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void loginValidUserSuccessfully() {
        User user = user(1L, Role.INTERN);
        LoginRequest request = loginRequest("INTERN@test.com", "123456");

        when(userRepository.findByEmailIgnoreCase("intern@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(1_800_000L);
        when(refreshTokenService.create(user, null, null)).thenReturn(refreshToken(user));
        when(refreshTokenService.getRefreshExpirationMs()).thenReturn(604_800_000L);
        when(userService.toUserInfo(user)).thenReturn(userInfo(user));

        AuthSession session = authService.login(request, null);
        LoginResponse response = session.getLoginResponse();

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(session.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(1_800_000L);
        assertThat(response.getUser().getEmail()).isEqualTo("intern@test.com");
        verify(auditLogService).record(user, "USER_LOGIN", "User", 1L);
    }

    @Test
    void rejectInvalidPassword() {
        User user = user(1L, Role.INTERN);
        LoginRequest request = loginRequest("intern@test.com", "wrong-password");

        when(userRepository.findByEmailIgnoreCase("intern@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, null))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void returnCurrentUserFromAuthentication() {
        User user = user(1L, Role.INTERN);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(userService.getCurrentUser(authentication)).thenReturn(user);
        when(userService.toUserInfo(user)).thenReturn(userInfo(user));

        LoginResponse.UserInfo response = authService.getCurrentUser(authentication);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo(Role.INTERN);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Aarav Mehta");
        request.setEmail(" INTERN@test.com ");
        request.setPassword("123456");
        request.setRole(Role.INTERN);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginResponse.UserInfo userInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status("ACTIVE")
                .active(true)
                .build();
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .name("Aarav Mehta")
                .email("intern@test.com")
                .password("encoded")
                .role(role)
                .active(true)
                .build();
    }

    private RefreshToken refreshToken(User user) {
        return RefreshToken.builder()
                .id(10L)
                .user(user)
                .token("refresh-token")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();
    }
}
