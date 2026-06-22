package com.interniq.notification;

import com.interniq.common.ApiResponse;
import com.interniq.notification.dto.CreateNotificationRequest;
import com.interniq.notification.dto.EmailSettingsResponse;
import com.interniq.notification.dto.NotificationResponse;
import com.interniq.notification.dto.TestEmailRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;
    private final NotificationService notificationService;

    @Value("${application.email.provider:mock}")
    private String emailProvider;

    @Value("${application.email.enabled:false}")
    private boolean mailEnabled;

    @Value("${application.email.from:no-reply@interniq.local}")
    private String fromEmail;

    @Value("${application.email.app-name:InternIQ}")
    private String appName;

    @Value("${application.email.app-url:http://localhost:5173}")
    private String appUrl;

    @Value("${application.email.fail-on-error:false}")
    private Boolean failOnError;

    @Value("${spring.mail.host:localhost}")
    private String smtpHost;

    @Value("${spring.mail.port:1025}")
    private Integer smtpPort;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private Boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private Boolean smtpStartTls;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Notifications loaded successfully", notificationService.getMyNotifications(authentication)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Unread count loaded successfully", Map.of("count", notificationService.getUnreadCount(authentication))));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notificationService.markAsRead(id, authentication)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> markAllAsRead(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Notifications marked as read", notificationService.markAllAsRead(authentication)));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<NotificationResponse>> archive(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Notification archived", notificationService.archive(id, authentication)));
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> createSystemNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("System notification created", notificationService.createSystemNotification(request, authentication)));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("All notifications loaded successfully", notificationService.getAllNotifications(authentication)));
    }

    @GetMapping("/email-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmailSettingsResponse>> getEmailSettings() {
        String provider = emailProvider == null || emailProvider.isBlank() ? "mock" : emailProvider.trim();
        EmailSettingsResponse response = EmailSettingsResponse.builder()
                .provider(provider.toUpperCase())
                .mailEnabled(mailEnabled)
                .realEmailEnabled(mailEnabled)
                .fromEmail(fromEmail)
                .appName(appName)
                .appUrl(appUrl)
                .smtpHost(maskIfBlank(smtpHost))
                .smtpPort(smtpPort)
                .smtpAuth(smtpAuth)
                .smtpStartTls(smtpStartTls)
                .failOnError(failOnError)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Email settings loaded successfully", response));
    }

    @PostMapping("/test-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        emailService.sendTestEmail(request.getRecipient().trim().toLowerCase());
        return ResponseEntity.ok(ApiResponse.success("Test email request processed successfully", null));
    }

    private String maskIfBlank(String value) {
        return value == null || value.isBlank() ? "Not configured" : value;
    }
}
