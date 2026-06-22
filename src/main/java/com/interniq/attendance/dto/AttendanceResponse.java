package com.interniq.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long id;
    private Long internId;
    private String internName;
    private LocalDate date;
    private LocalTime punchInTime;
    private LocalTime punchOutTime;
    private BigDecimal totalHours;
    private String status;
}
