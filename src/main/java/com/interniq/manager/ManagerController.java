package com.interniq.manager;

import com.interniq.common.ApiResponse;
import com.interniq.manager.dto.ManagerDtos.*;
import com.interniq.task.Priority;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final ManagerService managerService;

    @GetMapping("/dashboard")
    public ApiResponse<ManagerDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success("Manager dashboard loaded", managerService.getDashboard(authentication));
    }

    @GetMapping("/interns")
    public ApiResponse<List<InternCardResponse>> assignedInterns(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String subDepartment,
            @RequestParam(required = false) String assignedCompany,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        return ApiResponse.success("Assigned interns loaded",
                managerService.getAssignedInterns(search, department, subDepartment, assignedCompany, status, authentication));
    }

    @GetMapping("/interns/{internId}")
    public ApiResponse<InternCardResponse> assignedIntern(@PathVariable Long internId, Authentication authentication) {
        return ApiResponse.success("Assigned intern loaded", managerService.getAssignedIntern(internId, authentication));
    }

    @GetMapping("/interns/{internId}/attendance")
    public ApiResponse<List<AttendanceRecordResponse>> internAttendance(
            @PathVariable Long internId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        return ApiResponse.success("Intern attendance loaded",
                managerService.getInternAttendance(internId, from, to, status, authentication));
    }

    @GetMapping("/interns/{internId}/attendance/summary")
    public ApiResponse<AttendanceSummaryResponse> internAttendanceSummary(
            @PathVariable Long internId,
            Authentication authentication) {
        return ApiResponse.success("Intern attendance summary loaded",
                managerService.getInternAttendanceSummary(internId, authentication));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ManagerTaskResponse>> tasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long internId,
            Authentication authentication) {
        return ApiResponse.success("Manager tasks loaded",
                managerService.getTasks(status, priority, internId, authentication));
    }

    @PostMapping("/tasks")
    public ApiResponse<ManagerTaskResponse> createTask(
            @Valid @RequestBody ManagerTaskRequest request,
            Authentication authentication) {
        return ApiResponse.success("Task assigned successfully", managerService.createTask(request, authentication));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<ManagerTaskResponse> task(@PathVariable Long taskId, Authentication authentication) {
        return ApiResponse.success("Task loaded", managerService.getTask(taskId, authentication));
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse<ManagerTaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody ManagerTaskRequest request,
            Authentication authentication) {
        return ApiResponse.success("Task updated successfully", managerService.updateTask(taskId, request, authentication));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId, Authentication authentication) {
        managerService.deleteTask(taskId, authentication);
        return ApiResponse.success("Task deleted successfully", null);
    }

    @GetMapping("/tasks/submitted")
    public ApiResponse<List<ManagerTaskResponse>> submittedTasks(Authentication authentication) {
        return ApiResponse.success("Submitted tasks loaded", managerService.getSubmittedTasks(authentication));
    }

    @GetMapping("/tasks/reviewed")
    public ApiResponse<List<ManagerTaskResponse>> reviewedTasks(Authentication authentication) {
        return ApiResponse.success("Reviewed tasks loaded", managerService.getReviewedTasks(authentication));
    }

    @PutMapping("/tasks/{taskId}/review")
    public ApiResponse<ManagerTaskResponse> reviewTask(
            @PathVariable Long taskId,
            @Valid @RequestBody ManagerTaskReviewRequest request,
            Authentication authentication) {
        return ApiResponse.success("Task reviewed successfully", managerService.reviewTask(taskId, request, authentication));
    }

    @GetMapping("/interns/{internId}/feedback")
    public ApiResponse<List<ManagerFeedbackResponse>> internFeedback(
            @PathVariable Long internId,
            Authentication authentication) {
        return ApiResponse.success("Intern feedback loaded", managerService.getInternFeedback(internId, authentication));
    }

    @PostMapping("/interns/{internId}/feedback")
    public ApiResponse<ManagerFeedbackResponse> createInternFeedback(
            @PathVariable Long internId,
            @Valid @RequestBody ManagerFeedbackRequest request,
            Authentication authentication) {
        return ApiResponse.success("Feedback added successfully",
                managerService.createInternFeedback(internId, request, authentication));
    }

    @GetMapping("/reports/summary")
    public ApiResponse<ManagerReportResponse> reportSummary(Authentication authentication) {
        return ApiResponse.success("Manager reports loaded", managerService.getReportSummary(authentication));
    }

    @GetMapping("/interns/{internId}/report")
    public ApiResponse<InternReportResponse> internReport(
            @PathVariable Long internId,
            Authentication authentication) {
        return ApiResponse.success("Intern report loaded", managerService.getInternReport(internId, authentication));
    }

    @GetMapping("/interview-results")
    public ApiResponse<List<InterviewResultResponse>> interviewResults(Authentication authentication) {
        return ApiResponse.success("Interview results loaded", managerService.getInterviewResults(authentication));
    }

    @GetMapping("/interns/{internId}/interview-results")
    public ApiResponse<List<InterviewResultResponse>> internInterviewResults(
            @PathVariable Long internId,
            Authentication authentication) {
        return ApiResponse.success("Intern interview results loaded",
                managerService.getInterviewResultsForIntern(internId, authentication));
    }
}
