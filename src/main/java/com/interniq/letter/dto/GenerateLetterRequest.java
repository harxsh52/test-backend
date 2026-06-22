package com.interniq.letter.dto;

import com.interniq.letter.LetterType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GenerateLetterRequest {

    @NotBlank(message = "Candidate name is required")
    @Size(max = 180, message = "Candidate name must be at most 180 characters")
    private String candidateName;

    @NotBlank(message = "Candidate email is required")
    @Email(message = "Candidate email must be valid")
    private String candidateEmail;

    private Long candidateId;

    private Long internId;

    @NotNull(message = "Letter type is required")
    private LetterType letterType;

    @NotBlank(message = "Role name is required")
    @Size(max = 180, message = "Role name must be at most 180 characters")
    private String roleName;

    private String department;

    private LocalDate joiningDate;

    private LocalDate internshipStartDate;

    private LocalDate internshipEndDate;

    private BigDecimal stipend;

    private String workLocation;

    private String reportingManager;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String hrName;
}
