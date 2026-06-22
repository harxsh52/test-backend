package com.interniq.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackRequest {

    @NotNull(message = "Intern is required")
    private Long internId;

    private Long taskId;

    @NotBlank(message = "Feedback text is required")
    @Size(max = 3000, message = "Feedback text must be at most 3000 characters")
    private String feedbackText;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
}
