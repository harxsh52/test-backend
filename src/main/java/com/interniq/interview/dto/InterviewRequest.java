package com.interniq.interview.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InterviewRequest {

    private Long candidateId;

    private Long internId;

    @Size(max = 160, message = "Role must be at most 160 characters")
    private String role;

    @FutureOrPresent(message = "Scheduled time cannot be in the past")
    private LocalDateTime scheduledAt;
}
