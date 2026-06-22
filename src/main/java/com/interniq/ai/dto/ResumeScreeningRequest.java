package com.interniq.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeScreeningRequest {

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;
}
