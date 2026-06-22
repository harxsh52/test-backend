package com.interniq.intern.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternProfileResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    private String phone;
    private String college;
    private String skills;
    private LocalDate joiningDate;
    private LocalDate internshipStartDate;
    private LocalDate internshipEndDate;
    private String status;
}
