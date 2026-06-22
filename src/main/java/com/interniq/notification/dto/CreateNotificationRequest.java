package com.interniq.notification.dto;

import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationType;
import com.interniq.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateNotificationRequest {

    private List<Long> userIds;

    private List<Role> roles;

    @NotBlank(message = "Title is required")
    @Size(max = 180, message = "Title must be at most 180 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    private String message;

    private NotificationType type = NotificationType.SYSTEM;

    private NotificationPriority priority = NotificationPriority.MEDIUM;

    @Size(max = 1000, message = "Action URL must be at most 1000 characters")
    private String actionUrl;

    @Size(max = 5000, message = "Metadata must be at most 5000 characters")
    private String metadataJson;
}
