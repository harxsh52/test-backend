package com.interniq.candidate.dto;

import com.interniq.candidate.CandidateStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 160, message = "Name must be at most 160 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;

    @NotBlank(message = "Applied role is required")
    @Size(max = 160, message = "Applied role must be at most 160 characters")
    private String appliedRole;

    @Size(max = 1500, message = "Skills must be at most 1500 characters")
    private String skills;

    @Size(max = 255, message = "Resume file name must be at most 255 characters")
    private String resumeFileName;

    private CandidateStatus status;
}
