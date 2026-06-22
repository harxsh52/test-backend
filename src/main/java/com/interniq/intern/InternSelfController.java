package com.interniq.intern;

import com.interniq.common.ApiResponse;
import com.interniq.intern.dto.InternSelfDtos.AttendanceRecordResponse;
import com.interniq.intern.dto.InternSelfDtos.AttendanceSummaryResponse;
import com.interniq.intern.dto.InternSelfDtos.FeedbackPageResponse;
import com.interniq.intern.dto.InternSelfDtos.InternDashboardResponse;
import com.interniq.intern.dto.InternSelfDtos.InternInterviewResponse;
import com.interniq.intern.dto.InternSelfDtos.InternManagerFeedbackResponse;
import com.interniq.intern.dto.InternSelfDtos.InternProfileResponse;
import com.interniq.intern.dto.InternSelfDtos.InternReportResponse;
import com.interniq.intern.dto.InternSelfDtos.InternTaskResponse;
import com.interniq.intern.dto.InternSelfDtos.InternTaskSubmitRequest;
import com.interniq.intern.dto.InternSelfDtos.ManagerFeedbackRequest;
import com.interniq.intern.dto.InternSelfDtos.UpdateInternProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/intern")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERN')")
public class InternSelfController {

    private final InternSelfService internSelfService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<InternDashboardResponse>> dashboard(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Intern dashboard loaded successfully", internSelfService.getDashboard(authentication)));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<InternProfileResponse>> profile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Intern profile loaded successfully", internSelfService.getProfile(authentication)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<InternProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateInternProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Intern profile updated successfully", internSelfService.updateProfile(authentication, request)));
    }

    @GetMapping("/attendance/today")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> todayAttendance(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Today's attendance loaded successfully", internSelfService.getTodayAttendance(authentication)));
    }

    @GetMapping("/attendance/history")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> attendanceHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Attendance history loaded successfully", internSelfService.getAttendanceHistory(fromDate, toDate, authentication)));
    }

    @GetMapping("/attendance/summary")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> attendanceSummary(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Attendance summary loaded successfully", internSelfService.getAttendanceSummary(authentication)));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<InternTaskResponse>>> tasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Intern tasks loaded successfully", internSelfService.getTasks(status, priority, dueDate, search, authentication)));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<InternTaskResponse>> task(@PathVariable Long taskId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Intern task loaded successfully", internSelfService.getTask(taskId, authentication)));
    }

    @PutMapping("/tasks/{taskId}/start")
    public ResponseEntity<ApiResponse<InternTaskResponse>> startTask(@PathVariable Long taskId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Task started successfully", internSelfService.startTask(taskId, authentication)));
    }

    @PostMapping("/tasks/{taskId}/submit")
    public ResponseEntity<ApiResponse<InternTaskResponse>> submitTask(
            @PathVariable Long taskId,
            @Valid @RequestBody InternTaskSubmitRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Task submitted successfully", internSelfService.submitTask(taskId, request, authentication)));
    }

    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<FeedbackPageResponse>> feedback(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Intern feedback loaded successfully", internSelfService.getFeedback(authentication)));
    }

    @PostMapping("/manager-feedback")
    public ResponseEntity<ApiResponse<InternManagerFeedbackResponse>> managerFeedback(
            @Valid @RequestBody ManagerFeedbackRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Manager feedback submitted successfully", internSelfService.submitManagerFeedback(request, authentication)));
    }

    @GetMapping("/report")
    public ResponseEntity<ApiResponse<InternReportResponse>> report(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Intern report loaded successfully", internSelfService.getReport(authentication)));
    }

    @GetMapping("/interviews")
    public ResponseEntity<ApiResponse<List<InternInterviewResponse>>> interviews(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Intern interviews loaded successfully", internSelfService.getInterviews(authentication)));
    }

    @GetMapping("/interview-results")
    public ResponseEntity<ApiResponse<List<InternInterviewResponse>>> interviewResults(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Intern interview results loaded successfully", internSelfService.getInterviewResults(authentication)));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<String>>> notifications() {
        return ResponseEntity.ok(ApiResponse.success("Intern notifications loaded successfully", List.of()));
    }
}
