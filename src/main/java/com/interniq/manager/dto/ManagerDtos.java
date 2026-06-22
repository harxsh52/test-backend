package com.interniq.manager.dto;

import com.interniq.task.Priority;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class ManagerDtos {
    private ManagerDtos() {}

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ManagerProfileResponse {
        private Long id;
        private String empId;
        private String name;
        private String email;
        private String phone;
        private String profileImageUrl;
        private String department;
        private String subDepartment;
        private String assignedCompany;
        private String designation;
        private String status;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ManagerDashboardResponse {
        private ManagerProfileResponse managerProfile;
        private SummaryCardsResponse summaryCards;
        private List<InternCardResponse> assignedInternsSummary;
        private TaskSummaryResponse taskSummary;
        private AttendanceSummaryResponse attendanceSummary;
        private List<ManagerTaskResponse> pendingReviews;
        private List<ManagerTaskResponse> recentSubmissions;
        private List<InternPerformanceResponse> topPerformers;
        private List<InternImprovementResponse> internsNeedingImprovement;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SummaryCardsResponse {
        private long totalAssignedInterns;
        private long activeInterns;
        private long tasksAssigned;
        private long tasksSubmitted;
        private long tasksCompleted;
        private long pendingReviews;
        private long overdueTasks;
        private long lowAttendanceInterns;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternCardResponse {
        private Long id;
        private Long internId;
        private Long userId;
        private String empId;
        private String name;
        private String email;
        private String phone;
        private String profileImageUrl;
        private String department;
        private String subDepartment;
        private String assignedCompany;
        private String college;
        private List<String> skills;
        private LocalDate joiningDate;
        private LocalDate internshipStartDate;
        private LocalDate internshipEndDate;
        private String status;
        private BigDecimal attendancePercentage;
        private BigDecimal taskProgressPercentage;
        private long pendingReviews;
        private BigDecimal averageRating;
        private BigDecimal finalScore;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceRecordResponse {
        private Long id;
        private Long internId;
        private String internName;
        private String empId;
        private LocalDate date;
        private LocalTime punchInTime;
        private LocalTime punchOutTime;
        private BigDecimal totalHours;
        private String totalWorkingHoursText;
        private String status;
        private String source;
        private BigDecimal attendancePercentage;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceSummaryResponse {
        private BigDecimal averageAttendancePercentage;
        private long presentToday;
        private long absentToday;
        private long halfDayToday;
        private long leaveToday;
        private long presentDays;
        private long absentDays;
        private long totalRecordedDays;
        private BigDecimal totalWorkingHours;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaskSummaryResponse {
        private long assigned;
        private long inProgress;
        private long submitted;
        private long reviewed;
        private long completed;
        private long rejected;
        private long overdue;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ManagerTaskResponse {
        private Long id;
        private String title;
        private String description;
        private String taskCategory;
        private String expectedOutput;
        private String referenceLink;
        private String attachmentUrl;
        private Priority priority;
        private String status;
        private Long assignedToInternId;
        private String assignedToName;
        private String assignedToEmpId;
        private Long assignedByManagerId;
        private String assignedByName;
        private LocalDate dueDate;
        private LocalDateTime assignedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String submissionText;
        private String submissionNote;
        private String githubLink;
        private String deploymentLink;
        private String attachmentSubmissionUrl;
        private String submissionLink;
        private String managerFeedback;
        private Integer rating;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private boolean overdue;
    }

    @Getter @Setter
    public static class ManagerTaskRequest {
        @NotNull private Long assignedToInternId;
        @NotBlank @Size(max = 180) private String title;
        @Size(max = 4000) private String description;
        private Priority priority;
        @FutureOrPresent private LocalDate dueDate;
        @Size(max = 120) private String taskCategory;
        @Size(max = 1500) private String expectedOutput;
        @Size(max = 1000) private String referenceLink;
        @Size(max = 1000) private String attachmentUrl;
    }

    @Getter @Setter
    public static class ManagerTaskReviewRequest {
        @NotBlank private String reviewStatus;
        @Min(1) @Max(5) private Integer rating;
        @Size(max = 2500) private String feedback;
        @Size(max = 2000) private String strengths;
        @Size(max = 2000) private String improvementAreas;
    }

    @Getter @Setter
    public static class ManagerFeedbackRequest {
        private Long taskId;
        @Min(1) @Max(5) private Integer ratingOverall;
        @Min(1) @Max(5) private Integer ratingTechnical;
        @Min(1) @Max(5) private Integer ratingCommunication;
        @Min(1) @Max(5) private Integer ratingDiscipline;
        @Min(1) @Max(5) private Integer ratingTaskQuality;
        private String strengths;
        private String improvementAreas;
        @Size(max = 3000) private String comment;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ManagerFeedbackResponse {
        private Long id;
        private Long internId;
        private String internName;
        private String empId;
        private Long taskId;
        private String taskTitle;
        private Integer ratingOverall;
        private Integer ratingTechnical;
        private Integer ratingCommunication;
        private Integer ratingDiscipline;
        private Integer ratingTaskQuality;
        private String strengths;
        private String improvementAreas;
        private String comment;
        private LocalDateTime createdAt;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternPerformanceResponse {
        private Long internId;
        private String internName;
        private String empId;
        private BigDecimal finalScore;
        private BigDecimal attendancePercentage;
        private BigDecimal taskCompletionPercentage;
        private BigDecimal averageRating;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternImprovementResponse {
        private Long internId;
        private String internName;
        private String empId;
        private String reason;
        private BigDecimal attendancePercentage;
        private long pendingTasks;
        private long overdueTasks;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ManagerReportResponse {
        private SummaryCardsResponse summaryCards;
        private AttendanceSummaryResponse attendanceSummary;
        private TaskSummaryResponse taskSummary;
        private List<InternPerformanceResponse> topPerformers;
        private List<InternImprovementResponse> internsNeedingImprovement;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternReportResponse {
        private InternCardResponse intern;
        private AttendanceSummaryResponse attendanceSummary;
        private TaskSummaryResponse taskSummary;
        private List<ManagerFeedbackResponse> feedback;
        private List<InterviewResultResponse> interviewResults;
        private BigDecimal finalScore;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InterviewResultResponse {
        private Long id;
        private Long internId;
        private String internName;
        private String empId;
        private String role;
        private String status;
        private LocalDateTime completedAt;
        private Integer finalScore;
        private String recommendation;
        private String aiSummary;
    }
}
