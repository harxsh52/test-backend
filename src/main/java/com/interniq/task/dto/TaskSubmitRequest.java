package com.interniq.task.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskSubmitRequest {

    @Size(max = 1000, message = "Submission link must be at most 1000 characters")
    private String submissionLink;

    @Size(max = 2000, message = "Submission note must be at most 2000 characters")
    private String submissionNote;
}
