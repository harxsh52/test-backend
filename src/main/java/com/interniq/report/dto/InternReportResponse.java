package com.interniq.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternReportResponse {

    private Long internId;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String college;
    private String skills;
    private String status;
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    private LocalDate joiningDate;
    private LocalDate internshipStartDate;
    private LocalDate internshipEndDate;

    private Long totalWorkingDays;
    private BigDecimal totalWorkingHours;
    private BigDecimal attendancePercentage;

    private Long tasksAssigned;
    private Long tasksCompleted;
    private Long pendingTasks;
    private Long submittedTasks;
    private BigDecimal averageTaskRating;

    private Long feedbackCount;
    private BigDecimal averageFeedbackRating;
    private List<String> managerFeedback;

    private BigDecimal finalExperienceScore;
    private String overallExperienceSummary;
}
