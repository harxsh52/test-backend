package com.interniq.report;

import com.interniq.common.ApiResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.report.dto.AttendanceReportResponse;
import com.interniq.report.dto.DashboardStatsResponse;
import com.interniq.report.dto.DepartmentReportResponse;
import com.interniq.report.dto.InternReportResponse;
import com.interniq.report.dto.TaskReportResponse;
import com.interniq.task.Priority;
import com.interniq.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats loaded successfully", reportService.getDashboardStats(authentication)));
    }

    @GetMapping("/intern/{internId}")
    public ResponseEntity<ApiResponse<InternReportResponse>> getInternReport(
            @PathVariable Long internId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Intern report loaded successfully", reportService.getInternReport(internId, authentication)));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<AttendanceReportResponse>> getAttendanceReport(
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Attendance report loaded successfully",
                reportService.getAttendanceReport(internId, departmentId, fromDate, toDate, page, size, sortBy, sortDirection, authentication)
        ));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<TaskReportResponse>> getTaskReport(
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Task report loaded successfully",
                reportService.getTaskReport(internId, managerId, status, priority, fromDate, toDate, page, size, sortBy, sortDirection, authentication)
        ));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<?>> getDepartmentReports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "departmentName") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Department reports loaded successfully",
                    reportService.searchDepartmentReports(page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success("Department reports loaded successfully", reportService.getDepartmentReports(authentication)));
    }
}
