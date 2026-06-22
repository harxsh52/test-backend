package com.interniq.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentReportResponse {

    private Long departmentId;
    private String departmentName;
    private Long totalInterns;
    private Long activeInterns;
    private BigDecimal averageAttendance;
    private BigDecimal averageTaskCompletion;
    private BigDecimal averageRating;
}
