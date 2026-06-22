package com.interniq.admin.dto;

import com.interniq.task.Priority;
import com.interniq.task.TaskStatus;
import com.interniq.user.Role;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class AdminDtos {
    private AdminDtos() {
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AdminDashboardResponse {
        private SummaryCards summaryCards;
        private Map<String, Object> analytics;
        private List<InternScoreResponse> topInterns;
        private List<InternScoreResponse> lowPerformingInterns;
        private List<ManagerPerformanceResponse> managerPerformance;
        private List<ActivityResponse> recentActivity;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SummaryCards {
        private long totalUsers;
        private long totalInterns;
        private long totalManagers;
        private long totalHR;
        private long totalCandidates;
        private long activeInterns;
        private long completedInternships;
        private long totalDepartments;
        private long totalAssignedCompanies;
        private long totalTasks;
        private long pendingReviews;
        private BigDecimal averageAttendance;
        private BigDecimal averageInternScore;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserAdminResponse {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String empId;
        private String profileImageUrl;
        private Role role;
        private String department;
        private String managerName;
        private String designation;
        private String status;
        private boolean active;
        private boolean accountLocked;
        private int failedLoginAttempts;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter @Setter
    public static class UserAdminRequest {
        @NotBlank @Size(max = 120) private String name;
        @NotBlank @Email @Size(max = 180) private String email;
        @Size(max = 30) private String phone;
        @Size(max = 40) private String empId;
        @Size(max = 1000) private String profileImageUrl;
        @NotNull private Role role;
        @Size(max = 120) private String department;
        @Size(max = 120) private String managerName;
        @Size(min = 6, max = 100) private String password;
        private String status;
    }

    @Getter @Setter
    public static class StatusRequest {
        @NotBlank private String status;
    }

    @Getter @Setter
    public static class RoleRequest {
        @NotNull private Role role;
    }

    @Getter @Setter
    public static class ResetPasswordRequest {
        @Size(min = 6, max = 100) private String password;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ResetPasswordResponse {
        private Long userId;
        private String email;
        private String temporaryPassword;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternAdminResponse {
        private Long id;
        private Long userId;
        private String empId;
        private String name;
        private String email;
        private String phone;
        private Long departmentId;
        private String departmentName;
        private String subDepartment;
        private String assignedCompany;
        private Long managerId;
        private String managerName;
        private String college;
        private String skills;
        private LocalDate joiningDate;
        private LocalDate internshipStartDate;
        private LocalDate internshipEndDate;
        private String status;
        private BigDecimal attendancePercentage;
        private BigDecimal taskCompletionPercentage;
        private BigDecimal averageRating;
        private BigDecimal finalScore;
    }

    @Getter @Setter
    public static class InternAdminRequest {
        private Long departmentId;
        private Long managerId;
        @Size(max = 30) private String phone;
        @Size(max = 120) private String empId;
        @Size(max = 120) private String subDepartment;
        @Size(max = 120) private String assignedCompany;
        @Size(max = 180) private String college;
        @Size(max = 1500) private String skills;
        private LocalDate joiningDate;
        private LocalDate internshipStartDate;
        private LocalDate internshipEndDate;
        private String status;
    }

    @Getter @Setter
    public static class AssignManagerRequest {
        @NotNull private Long managerId;
    }

    @Getter @Setter
    public static class AssignDepartmentRequest {
        @NotNull private Long departmentId;
        @Size(max = 120) private String subDepartment;
        @Size(max = 120) private String assignedCompany;
    }

    @Getter @Setter
    public static class DepartmentAdminRequest {
        @NotBlank @Size(max = 120) private String name;
        @Size(max = 1000) private String description;
        private String status;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepartmentAdminResponse {
        private Long id;
        private String name;
        private String description;
        private String status;
        private long totalInterns;
        private long activeInterns;
    }

    @Getter @Setter
    public static class CatalogRequest {
        @NotNull private Long departmentId;
        @NotBlank @Size(max = 120) private String name;
        @Size(max = 1000) private String description;
        private String status;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CatalogResponse {
        private Long id;
        private Long departmentId;
        private String departmentName;
        private String name;
        private String description;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceAdminResponse {
        private Long id;
        private Long internId;
        private String internName;
        private String department;
        private LocalDate date;
        private LocalTime punchInTime;
        private LocalTime punchOutTime;
        private BigDecimal totalHours;
        private String status;
        private String source;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaskAdminResponse {
        private Long id;
        private String title;
        private String description;
        private Long assignedToInternId;
        private String assignedToName;
        private Long assignedById;
        private String assignedByName;
        private Priority priority;
        private TaskStatus status;
        private LocalDate dueDate;
        private Integer rating;
        private LocalDateTime createdAt;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FeedbackAdminResponse {
        private Long id;
        private Long internId;
        private String internName;
        private Long managerId;
        private String managerName;
        private Long taskId;
        private String taskTitle;
        private Integer rating;
        private String feedbackText;
        private LocalDateTime createdAt;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InterviewAdminResponse {
        private Long id;
        private Long candidateId;
        private String candidateName;
        private Long internId;
        private String internName;
        private String role;
        private String status;
        private LocalDateTime scheduledAt;
        private LocalDateTime completedAt;
        private Integer finalScore;
        private String recommendation;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InterviewResultAdminResponse {
        private Long id;
        private Long interviewId;
        private String participantName;
        private Integer technicalScore;
        private Integer communicationScore;
        private Integer problemSolvingScore;
        private Integer confidenceScore;
        private Integer finalScore;
        private String recommendation;
        private String aiSummary;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InternScoreResponse {
        private Long internId;
        private String empId;
        private String name;
        private String department;
        private String assignedCompany;
        private String reason;
        private BigDecimal attendancePercentage;
        private BigDecimal taskCompletionPercentage;
        private BigDecimal finalScore;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ManagerPerformanceResponse {
        private Long managerId;
        private String name;
        private String department;
        private long assignedInterns;
        private BigDecimal averageInternScore;
        private long pendingReviews;
        private BigDecimal averageFeedbackRating;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActivityResponse {
        private String action;
        private String actorName;
        private String entityType;
        private String description;
        private LocalDateTime createdAt;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SummaryResponse {
        private Map<String, Object> metrics;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SearchResultResponse {
        private String type;
        private Long id;
        private String title;
        private String subtitle;
        private String status;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SettingResponse {
        private String key;
        private String value;
        private String description;
    }

    @Getter @Setter
    public static class SettingRequest {
        @NotBlank private String key;
        @Size(max = 2000) private String value;
        @Size(max = 500) private String description;
    }
}
