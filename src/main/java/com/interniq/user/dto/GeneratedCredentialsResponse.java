package com.interniq.user.dto;

import com.interniq.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedCredentialsResponse {

    private Long userId;
    private String name;
    private String email;
    private Role role;
    private String temporaryPassword;
}
