package com.interniq.task.dto;

import com.interniq.task.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskReviewRequest {

    @NotNull(message = "Review status is required")
    private TaskStatus status;

    @Size(max = 2000, message = "Manager feedback must be at most 2000 characters")
    private String managerFeedback;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
}
