package com.interniq.audit.dto;

import com.interniq.audit.LoginAuditStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuditLogResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String email;
    private LoginAuditStatus status;
    private String ipAddress;
    private String userAgent;
    private String failureReason;
    private LocalDateTime createdAt;
}
