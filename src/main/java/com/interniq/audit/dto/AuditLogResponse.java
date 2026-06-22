package com.interniq.audit.dto;

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
public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String actorName;
    private String actionType;
    private String entityName;
    private Long entityId;
    private LocalDateTime timestamp;
}
