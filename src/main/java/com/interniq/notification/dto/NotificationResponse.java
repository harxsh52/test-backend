package com.interniq.notification.dto;

import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationStatus;
import com.interniq.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long recipientUserId;
    private String recipientName;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private NotificationStatus status;
    private String actionUrl;
    private String metadataJson;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime archivedAt;
}
