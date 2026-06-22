package com.interniq.intern.dto;

import com.interniq.interview.InterviewStatus;
import com.interniq.task.Priority;
import com.interniq.task.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class InternSelfDtos {

    private InternSelfDtos() {
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternProfileResponse {
        private Long id;
        private Long userId;
        private String empId;
        private String name;
        private String email;
        private String phone;
        private String profileImageUrl;
        private String designation;
        private String department;
        private String subDepartment;
        private String assignedCompany;
        private String managerName;
        private String college;
        private List<String> skills;
        private LocalDate joiningDate;
        private LocalDate internshipStartDate;
        private LocalDate internshipEndDate;
        private String internshipType;
        private BigDecimal stipend;
        private String status;
    }

    @Getter
    @Setter
    public static class UpdateInternProfileRequest {
        @Size(max = 30, message = "Phone must be at most 30 characters")
        private String phone;

        @Size(max = 1000, message = "Profile image URL must be at most 1000 characters")
        private String profileImageUrl;

        private List<@Size(max = 80, message = "Each skill must be at most 80 characters") String> skills;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRecordResponse {
        private Long id;
        private String empId;
        private LocalDate attendanceDate;
        private LocalDate date;
        private LocalTime punchInTime;
        private LocalTime punchOutTime;
        private Integer totalWorkingMinutes;
        private String totalWorkingHoursText;
        private String status;
        private String source;
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummaryResponse {
        private Long presentDays;
        private Long absentDays;
        private Long halfDays;
        private Long leaveDays;
        private Long totalRecordedDays;
        private BigDecimal attendancePercentage;
        private Integer totalWorkingMinutes;
        private String totalWorkingHoursText;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSummaryResponse {
        private Long totalTasks;
        private Long assignedTasks;
        private Long inProgressTasks;
        private Long submittedTasks;
        private Long reviewedTasks;
        private Long completedTasks;
        private Long pendingTasks;
        private Long overdueTasks;
        private Long upcomingDueTasks;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackSummaryResponse {
        private BigDecimal averageRating;
        private Long totalFeedbacks;
        private String latestFeedbackComment;
        private LocalDateTime latestFeedbackDate;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportSummaryResponse {
        private BigDecimal finalScore;
        private BigDecimal attendanceScore;
        private BigDecimal taskScore;
        private BigDecimal managerRatingScore;
        private BigDecimal interviewScore;
        private String scoreMessage;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternDashboardResponse {
        private InternProfileResponse internProfile;
        private AttendanceRecordResponse todayAttendance;
        private AttendanceSummaryResponse attendanceSummary;
        private TaskSummaryResponse taskSummary;
        private FeedbackSummaryResponse feedbackSummary;
        private ReportSummaryResponse reportSummary;
        private List<InternTaskResponse> latestTasks;
        private List<ManagerFeedbackResponse> recentFeedback;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternTaskResponse {
        private Long id;
        private String title;
        private String description;
        private Priority priority;
        private TaskStatus status;
        private String displayStatus;
        private Long assignedToInternId;
        private String assignedToName;
        private Long assignedByManagerId;
        private String assignedByName;
        private LocalDate dueDate;
        private LocalDateTime assignedAt;
        private LocalDateTime createdAt;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private String submissionText;
        private String submissionNote;
        private String githubLink;
        private String deploymentLink;
        private String attachmentUrl;
        private String submissionLink;
        private String managerFeedback;
        private Integer rating;
        private String reviewStatus;
        private boolean overdue;
    }

    @Getter
    @Setter
    public static class InternTaskSubmitRequest {
        @Size(max = 3000, message = "Submission text must be at most 3000 characters")
        private String submissionText;

        @Size(max = 1000, message = "GitHub link must be at most 1000 characters")
        private String githubLink;

        @Size(max = 1000, message = "Deployment link must be at most 1000 characters")
        private String deploymentLink;

        @Size(max = 1000, message = "Attachment URL must be at most 1000 characters")
        private String attachmentUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerFeedbackResponse {
        private Long id;
        private Long taskId;
        private String taskName;
        private String managerName;
        private String feedback;
        private String feedbackText;
        private Integer rating;
        private String strengths;
        private String improvementAreas;
        private LocalDateTime createdAt;
        private LocalDateTime reviewDate;
    }

    @Getter
    @Setter
    public static class ManagerFeedbackRequest {
        @Min(value = 1, message = "Support rating must be at least 1")
        @Max(value = 5, message = "Support rating must be at most 5")
        private Integer ratingSupport;

        @Min(value = 1, message = "Communication rating must be at least 1")
        @Max(value = 5, message = "Communication rating must be at most 5")
        private Integer ratingCommunication;

        @Min(value = 1, message = "Guidance rating must be at least 1")
        @Max(value = 5, message = "Guidance rating must be at most 5")
        private Integer ratingGuidance;

        @Min(value = 1, message = "Availability rating must be at least 1")
        @Max(value = 5, message = "Availability rating must be at most 5")
        private Integer ratingAvailability;

        @Size(max = 2500, message = "Comment must be at most 2500 characters")
        private String comment;

        private Boolean anonymous;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackPageResponse {
        private List<ManagerFeedbackResponse> managerFeedback;
        private List<InternManagerFeedbackResponse> feedbackGivenToManager;
        private FeedbackSummaryResponse summary;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternManagerFeedbackResponse {
        private Long id;
        private String managerName;
        private Integer ratingSupport;
        private Integer ratingCommunication;
        private Integer ratingGuidance;
        private Integer ratingAvailability;
        private String comment;
        private Boolean anonymous;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternReportResponse {
        private InternProfileResponse profile;
        private AttendanceSummaryResponse attendanceSummary;
        private TaskSummaryResponse taskSummary;
        private FeedbackSummaryResponse feedbackSummary;
        private ReportSummaryResponse reportSummary;
        private Long workingDays;
        private BigDecimal averageManagerRating;
        private List<String> strengths;
        private List<String> improvementAreas;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternInterviewResponse {
        private Long id;
        private String role;
        private InterviewStatus status;
        private LocalDateTime scheduledAt;
        private LocalDateTime completedAt;
        private Integer finalScore;
        private String recommendation;
        private String aiSummary;
    }
}
