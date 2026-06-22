package com.interniq.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interniq.auth.dto.AuthSession;
import com.interniq.auth.dto.LoginRequest;
import com.interniq.auth.dto.LoginResponse;
import com.interniq.auth.dto.RegisterRequest;
import com.interniq.exception.GlobalExceptionHandler;
import com.interniq.user.Role;
import com.interniq.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthService authService = Mockito.mock(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(authService);
        AuthController controller = new AuthController(authService);
        ReflectionTestUtils.setField(controller, "cookieSecure", false);
        ReflectionTestUtils.setField(controller, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(controller, "cookiePath", "/api/auth");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsCreatedUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Aarav Mehta");
        request.setEmail("intern@test.com");
        request.setPassword("123456");
        request.setRole(Role.INTERN);

        when(authService.register(any(RegisterRequest.class))).thenReturn(LoginResponse.UserInfo.builder()
                .id(1L)
                .name("Aarav Mehta")
                .email("intern@test.com")
                .role(Role.INTERN)
                .build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("intern@test.com"))
                .andExpect(jsonPath("$.data.role").value("INTERN"));
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("intern@test.com");
        request.setPassword("123456");

        when(authService.login(any(LoginRequest.class), any())).thenReturn(AuthSession.builder()
                .refreshToken("refresh-token")
                .refreshExpiresInMs(604_800_000L)
                .loginResponse(LoginResponse.builder()
                .token("jwt-token")
                .accessToken("jwt-token")
                .expiresIn(1_800_000L)
                .user(LoginResponse.UserInfo.builder()
                        .id(1L)
                        .email("intern@test.com")
                        .role(Role.INTERN)
                        .build())
                .build())
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.role").value("INTERN"));
    }

    @Test
    void rejectInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("intern@test.com");
        request.setPassword("wrong-password");

        when(authService.login(any(LoginRequest.class), any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void returnCurrentUserFromMe() throws Exception {
        User user = user();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authService.getCurrentUser(authentication)).thenReturn(LoginResponse.UserInfo.builder()
                .id(1L)
                .email("intern@test.com")
                .role(Role.INTERN)
                .build());

        mockMvc.perform(get("/api/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("intern@test.com"));
    }

    @Test
    void unauthorizedMeRequestShouldFail() throws Exception {
        when(authService.getCurrentUser(nullable(Authentication.class)))
                .thenThrow(new BadCredentialsException("No authenticated user found"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginValidationReturnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("not-an-email");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    private User user() {
        return User.builder()
                .id(1L)
                .name("Aarav Mehta")
                .email("intern@test.com")
                .password("encoded")
                .role(Role.INTERN)
                .active(true)
                .build();
    }
}
