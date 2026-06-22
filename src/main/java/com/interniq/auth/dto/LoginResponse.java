package com.interniq.auth.dto;

import com.interniq.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;

    private UserInfo user;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private Role role;
        private String department;
        private String manager;
        private String managerName;
        private String designation;
        private String status;
        private boolean active;
        private List<String> permissions;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
