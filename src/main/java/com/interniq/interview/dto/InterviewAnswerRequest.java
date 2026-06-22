package com.interniq.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewAnswerRequest {

    @NotNull(message = "Question is required")
    private Long questionId;

    @NotBlank(message = "Answer is required")
    @Size(max = 5000, message = "Answer must be at most 5000 characters")
    private String answerText;
}
