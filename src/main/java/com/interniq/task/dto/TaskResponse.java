package com.interniq.task.dto;

import com.interniq.task.Priority;
import com.interniq.task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private Long assignedToInternId;
    private String assignedToName;
    private Long assignedByUserId;
    private String assignedByName;
    private Priority priority;
    private TaskStatus status;
    private LocalDate dueDate;
    private String submissionLink;
    private String submissionNote;
    private String managerFeedback;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
