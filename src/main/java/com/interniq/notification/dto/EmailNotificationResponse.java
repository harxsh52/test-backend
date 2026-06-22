package com.interniq.notification.dto;

import com.interniq.notification.EmailNotificationStatus;
import com.interniq.notification.EmailNotificationType;
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
public class EmailNotificationResponse {

    private Long id;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String body;
    private EmailNotificationType notificationType;
    private String relatedEntityType;
    private Long relatedEntityId;
    private EmailNotificationStatus status;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
