package com.interniq.user;

import com.interniq.auth.dto.LoginResponse;
import com.interniq.common.ApiResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.user.dto.GeneratedCredentialsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Users loaded successfully",
                    userService.searchUsers(role, status, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success("Users loaded successfully", userService.getUsers(role, status, authentication)));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<?>> getUsersByRole(
            @PathVariable Role role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Users loaded successfully",
                    userService.searchUsers(role, status, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success("Users loaded successfully", userService.getUsers(role, status, authentication)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getMyProfile(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(ApiResponse.success("Current user loaded successfully", userService.toUserInfo(currentUser)));
    }

    @PostMapping("/{userId}/generate-credentials")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<GeneratedCredentialsResponse>> generateCredentials(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Credentials generated successfully", userService.generateCredentials(userId, authentication)));
    }
}
