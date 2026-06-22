package com.interniq.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportResponse {

    private Long totalRecords;
    private Long presentDays;
    private BigDecimal totalWorkingHours;
    private BigDecimal attendancePercentage;
    private List<Row> records;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private Long id;
        private Long internId;
        private String internName;
        private Long departmentId;
        private String departmentName;
        private LocalDate date;
        private LocalTime punchInTime;
        private LocalTime punchOutTime;
        private BigDecimal totalHours;
        private String status;
    }
}
