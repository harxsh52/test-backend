package com.interniq.report.dto;

import com.interniq.user.Role;
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
public class DashboardStatsResponse {

    private Role role;

    private Long totalTasks;
    private Long completedTasks;
    private Long pendingTasks;
    private BigDecimal attendancePercentage;
    private BigDecimal totalWorkingHours;
    private BigDecimal averageRating;
    private BigDecimal experienceScore;

    private Long totalInterns;
    private Long activeInterns;
    private Long tasksAssigned;
    private Long pendingReviews;
    private BigDecimal averageInternScore;

    private Long totalCandidates;
    private Long completedInternships;
    private BigDecimal averageAttendance;
    private Long departmentsCount;

    private Long totalUsers;
    private Long totalDepartments;
    private Long totalManagers;
    private Long totalHR;
    private Long activeUsers;
}
