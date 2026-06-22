package com.interniq.intern.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class InternProfileRequest {

    private Long userId;

    private Long departmentId;

    private Long managerId;

    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;

    @Size(max = 160, message = "College must be at most 160 characters")
    private String college;

    @Size(max = 1500, message = "Skills must be at most 1500 characters")
    private String skills;

    private LocalDate joiningDate;

    @FutureOrPresent(message = "Internship start date cannot be in the past")
    private LocalDate internshipStartDate;

    @FutureOrPresent(message = "Internship end date cannot be in the past")
    private LocalDate internshipEndDate;

    @Size(max = 60, message = "Status must be at most 60 characters")
    private String status;
}
