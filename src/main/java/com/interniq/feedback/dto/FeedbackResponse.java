package com.interniq.feedback.dto;

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
public class FeedbackResponse {

    private Long id;
    private Long internId;
    private String internName;
    private Long managerId;
    private String managerName;
    private Long taskId;
    private String taskTitle;
    private String feedbackText;
    private Integer rating;
    private LocalDateTime createdAt;
}
