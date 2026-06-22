package com.interniq.notification;

import com.interniq.common.ApiResponse;
import com.interniq.notification.dto.EmailNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final EmailNotificationService emailNotificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailNotificationResponse>>> getNotifications(
            @RequestParam(required = false) EmailNotificationType type,
            @RequestParam(required = false) EmailNotificationStatus status,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.success("Email notifications loaded successfully", emailNotificationService.getNotifications(type, status, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Email notification loaded successfully", emailNotificationService.getNotification(id)));
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> resend(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Email notification resent successfully", emailNotificationService.resend(id, authentication)));
    }
}
