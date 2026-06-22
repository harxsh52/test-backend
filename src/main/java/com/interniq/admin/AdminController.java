package com.interniq.admin;

import com.interniq.admin.dto.AdminDtos.*;
import com.interniq.audit.dto.LoginAuditLogResponse;
import com.interniq.candidate.Candidate;
import com.interniq.candidate.CandidateStatus;
import com.interniq.common.ApiResponse;
import com.interniq.feedback.InternManagerFeedback;
import com.interniq.task.Priority;
import com.interniq.task.TaskStatus;
import com.interniq.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success("Admin dashboard loaded", adminService.dashboard(authentication));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserAdminResponse>> users(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return ApiResponse.success("Users loaded", adminService.users(role, status, search, authentication));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserAdminResponse> user(@PathVariable Long userId, Authentication authentication) {
        return ApiResponse.success("User loaded", adminService.user(userId, authentication));
    }

    @PostMapping("/users")
    public ApiResponse<UserAdminResponse> createUser(@Valid @RequestBody UserAdminRequest request, Authentication authentication) {
        return ApiResponse.success("User created", adminService.createUser(request, authentication));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<UserAdminResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UserAdminRequest request, Authentication authentication) {
        return ApiResponse.success("User updated", adminService.updateUser(userId, request, authentication));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<UserAdminResponse> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody StatusRequest request, Authentication authentication) {
        return ApiResponse.success("User status updated", adminService.updateUserStatus(userId, request, authentication));
    }

    @PutMapping("/users/{userId}/role")
    public ApiResponse<UserAdminResponse> updateUserRole(@PathVariable Long userId, @Valid @RequestBody RoleRequest request, Authentication authentication) {
        return ApiResponse.success("User role updated", adminService.updateUserRole(userId, request, authentication));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long userId, @RequestBody(required = false) ResetPasswordRequest request, Authentication authentication) {
        return ApiResponse.success("Password reset", adminService.resetPassword(userId, request == null ? new ResetPasswordRequest() : request, authentication));
    }

    @PutMapping("/users/{userId}/lock")
    public ApiResponse<UserAdminResponse> lockUser(@PathVariable Long userId, Authentication authentication) {
        return ApiResponse.success("User account locked", adminService.lockUser(userId, authentication));
    }

    @PutMapping("/users/{userId}/unlock")
    public ApiResponse<UserAdminResponse> unlockUser(@PathVariable Long userId, Authentication authentication) {
        return ApiResponse.success("User account unlocked", adminService.unlockUser(userId, authentication));
    }

    @GetMapping("/interns")
    public ApiResponse<List<InternAdminResponse>> interns(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return ApiResponse.success("Interns loaded", adminService.interns(departmentId, managerId, status, search, authentication));
    }

    @GetMapping("/interns/{internId}")
    public ApiResponse<InternAdminResponse> intern(@PathVariable Long internId, Authentication authentication) {
        return ApiResponse.success("Intern loaded", adminService.intern(internId, authentication));
    }

    @PutMapping("/interns/{internId}")
    public ApiResponse<InternAdminResponse> updateIntern(@PathVariable Long internId, @Valid @RequestBody InternAdminRequest request, Authentication authentication) {
        return ApiResponse.success("Intern updated", adminService.updateIntern(internId, request, authentication));
    }

    @PutMapping("/interns/{internId}/assign-manager")
    public ApiResponse<InternAdminResponse> assignManager(@PathVariable Long internId, @Valid @RequestBody AssignManagerRequest request, Authentication authentication) {
        return ApiResponse.success("Manager assigned", adminService.assignManager(internId, request, authentication));
    }

    @PutMapping("/interns/{internId}/assign-department")
    public ApiResponse<InternAdminResponse> assignDepartment(@PathVariable Long internId, @Valid @RequestBody AssignDepartmentRequest request, Authentication authentication) {
        return ApiResponse.success("Department assigned", adminService.assignDepartment(internId, request, authentication));
    }

    @GetMapping("/managers")
    public ApiResponse<List<UserAdminResponse>> managers(Authentication authentication) {
        return ApiResponse.success("Managers loaded", adminService.managers(authentication));
    }

    @GetMapping("/managers/{managerId}")
    public ApiResponse<UserAdminResponse> manager(@PathVariable Long managerId, Authentication authentication) {
        return ApiResponse.success("Manager loaded", adminService.user(managerId, authentication));
    }

    @PostMapping("/managers")
    public ApiResponse<UserAdminResponse> createManager(@Valid @RequestBody UserAdminRequest request, Authentication authentication) {
        request.setRole(Role.MANAGER);
        return ApiResponse.success("Manager created", adminService.createUser(request, authentication));
    }

    @PutMapping("/managers/{managerId}")
    public ApiResponse<UserAdminResponse> updateManager(@PathVariable Long managerId, @Valid @RequestBody UserAdminRequest request, Authentication authentication) {
        request.setRole(Role.MANAGER);
        return ApiResponse.success("Manager updated", adminService.updateUser(managerId, request, authentication));
    }

    @GetMapping("/managers/{managerId}/interns")
    public ApiResponse<List<InternAdminResponse>> managerInterns(@PathVariable Long managerId, Authentication authentication) {
        return ApiResponse.success("Manager interns loaded", adminService.managerInterns(managerId, authentication));
    }

    @GetMapping("/hr-users")
    public ApiResponse<List<UserAdminResponse>> hrUsers(Authentication authentication) {
        return ApiResponse.success("HR users loaded", adminService.hrUsers(authentication));
    }

    @GetMapping("/hr-users/{hrId}")
    public ApiResponse<UserAdminResponse> hrUser(@PathVariable Long hrId, Authentication authentication) {
        return ApiResponse.success("HR user loaded", adminService.user(hrId, authentication));
    }

    @PostMapping("/hr-users")
    public ApiResponse<UserAdminResponse> createHrUser(@Valid @RequestBody UserAdminRequest request, Authentication authentication) {
        request.setRole(Role.HR);
        return ApiResponse.success("HR user created", adminService.createUser(request, authentication));
    }

    @PutMapping("/hr-users/{hrId}")
    public ApiResponse<UserAdminResponse> updateHrUser(@PathVariable Long hrId, @Valid @RequestBody UserAdminRequest request, Authentication authentication) {
        request.setRole(Role.HR);
        return ApiResponse.success("HR user updated", adminService.updateUser(hrId, request, authentication));
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentAdminResponse>> departments(Authentication authentication) {
        return ApiResponse.success("Departments loaded", adminService.departments(authentication));
    }

    @PostMapping("/departments")
    public ApiResponse<DepartmentAdminResponse> createDepartment(@Valid @RequestBody DepartmentAdminRequest request, Authentication authentication) {
        return ApiResponse.success("Department created", adminService.createDepartment(request, authentication));
    }

    @PutMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentAdminResponse> updateDepartment(@PathVariable Long departmentId, @Valid @RequestBody DepartmentAdminRequest request, Authentication authentication) {
        return ApiResponse.success("Department updated", adminService.updateDepartment(departmentId, request, authentication));
    }

    @DeleteMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentAdminResponse> deleteDepartment(@PathVariable Long departmentId, Authentication authentication) {
        return ApiResponse.success("Department disabled", adminService.deleteDepartment(departmentId, authentication));
    }

    @GetMapping("/sub-departments")
    public ApiResponse<List<CatalogResponse>> subDepartments(Authentication authentication) {
        return ApiResponse.success("Sub departments loaded", adminService.subDepartments(authentication));
    }

    @PostMapping("/sub-departments")
    public ApiResponse<CatalogResponse> createSubDepartment(@Valid @RequestBody CatalogRequest request, Authentication authentication) {
        return ApiResponse.success("Sub department created", adminService.createSubDepartment(request, authentication));
    }

    @PutMapping("/sub-departments/{subDepartmentId}")
    public ApiResponse<CatalogResponse> updateSubDepartment(@PathVariable Long subDepartmentId, @Valid @RequestBody CatalogRequest request, Authentication authentication) {
        return ApiResponse.success("Sub department updated", adminService.updateSubDepartment(subDepartmentId, request, authentication));
    }

    @DeleteMapping("/sub-departments/{subDepartmentId}")
    public ApiResponse<CatalogResponse> deleteSubDepartment(@PathVariable Long subDepartmentId, Authentication authentication) {
        return ApiResponse.success("Sub department disabled", adminService.deleteSubDepartment(subDepartmentId, authentication));
    }

    @GetMapping("/assigned-companies")
    public ApiResponse<List<CatalogResponse>> assignedCompanies(Authentication authentication) {
        return ApiResponse.success("Assigned companies loaded", adminService.assignedCompanies(authentication));
    }

    @PostMapping("/assigned-companies")
    public ApiResponse<CatalogResponse> createAssignedCompany(@Valid @RequestBody CatalogRequest request, Authentication authentication) {
        return ApiResponse.success("Assigned company created", adminService.createAssignedCompany(request, authentication));
    }

    @PutMapping("/assigned-companies/{companyId}")
    public ApiResponse<CatalogResponse> updateAssignedCompany(@PathVariable Long companyId, @Valid @RequestBody CatalogRequest request, Authentication authentication) {
        return ApiResponse.success("Assigned company updated", adminService.updateAssignedCompany(companyId, request, authentication));
    }

    @DeleteMapping("/assigned-companies/{companyId}")
    public ApiResponse<CatalogResponse> deleteAssignedCompany(@PathVariable Long companyId, Authentication authentication) {
        return ApiResponse.success("Assigned company disabled", adminService.deleteAssignedCompany(companyId, authentication));
    }

    @GetMapping("/attendance")
    public ApiResponse<List<AttendanceAdminResponse>> attendance(
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        return ApiResponse.success("Attendance loaded", adminService.attendance(internId, departmentId, fromDate, toDate, status, authentication));
    }

    @GetMapping("/attendance/summary")
    public ApiResponse<SummaryResponse> attendanceSummary(Authentication authentication) {
        return ApiResponse.success("Attendance summary loaded", adminService.attendanceSummary(authentication));
    }

    @PostMapping("/attendance/sync")
    public ApiResponse<SummaryResponse> syncAttendance(Authentication authentication) {
        return ApiResponse.success("Attendance sync queued", adminService.syncAttendance(authentication));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<TaskAdminResponse>> tasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) Long managerId,
            Authentication authentication) {
        return ApiResponse.success("Tasks loaded", adminService.tasks(status, priority, internId, managerId, authentication));
    }

    @GetMapping("/tasks/summary")
    public ApiResponse<SummaryResponse> tasksSummary(Authentication authentication) {
        return ApiResponse.success("Tasks summary loaded", adminService.tasksSummary(authentication));
    }

    @GetMapping("/feedback/summary")
    public ApiResponse<SummaryResponse> feedbackSummary(Authentication authentication) {
        return ApiResponse.success("Feedback summary loaded", adminService.feedbackSummary(authentication));
    }

    @GetMapping("/manager-feedback")
    public ApiResponse<List<FeedbackAdminResponse>> managerFeedback(Authentication authentication) {
        return ApiResponse.success("Manager feedback loaded", adminService.managerFeedback(authentication));
    }

    @GetMapping("/intern-manager-feedback")
    public ApiResponse<List<InternManagerFeedback>> internManagerFeedback(Authentication authentication) {
        return ApiResponse.success("Intern manager feedback loaded", adminService.internManagerFeedback(authentication));
    }

    @GetMapping("/candidates")
    public ApiResponse<List<Candidate>> candidates(
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) String role,
            Authentication authentication) {
        return ApiResponse.success("Candidates loaded", adminService.candidates(status, role, authentication));
    }

    @GetMapping("/candidates/summary")
    public ApiResponse<SummaryResponse> candidatesSummary(Authentication authentication) {
        return ApiResponse.success("Candidates summary loaded", adminService.candidatesSummary(authentication));
    }

    @GetMapping("/interviews")
    public ApiResponse<List<InterviewAdminResponse>> interviews(Authentication authentication) {
        return ApiResponse.success("Interviews loaded", adminService.interviews(authentication));
    }

    @GetMapping("/interview-results")
    public ApiResponse<List<InterviewResultAdminResponse>> interviewResults(Authentication authentication) {
        return ApiResponse.success("Interview results loaded", adminService.interviewResults(authentication));
    }

    @GetMapping("/interviews/summary")
    public ApiResponse<SummaryResponse> interviewsSummary(Authentication authentication) {
        return ApiResponse.success("Interviews summary loaded", adminService.interviewsSummary(authentication));
    }

    @GetMapping("/reports/summary")
    public ApiResponse<SummaryResponse> reportsSummary(Authentication authentication) {
        return ApiResponse.success("Reports summary loaded", adminService.reportsSummary(authentication));
    }

    @GetMapping({"/reports/departments", "/reports/interns", "/reports/managers", "/reports/attendance", "/reports/tasks", "/reports/feedback"})
    public ApiResponse<SummaryResponse> reports(Authentication authentication) {
        return ApiResponse.success("Admin report loaded", adminService.reportsSummary(authentication));
    }

    @GetMapping("/search")
    public ApiResponse<List<SearchResultResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            Authentication authentication) {
        return ApiResponse.success("Search results loaded", adminService.search(q, type, authentication));
    }

    @GetMapping("/settings")
    public ApiResponse<List<SettingResponse>> settings(Authentication authentication) {
        return ApiResponse.success("Settings loaded", adminService.settings(authentication));
    }

    @GetMapping("/login-audit-logs")
    public ApiResponse<List<LoginAuditLogResponse>> loginAuditLogs(Authentication authentication) {
        return ApiResponse.success("Login audit logs loaded", adminService.loginAuditLogs(authentication));
    }

    @PutMapping("/settings")
    public ApiResponse<List<SettingResponse>> updateSettings(@Valid @RequestBody List<SettingRequest> request, Authentication authentication) {
        return ApiResponse.success("Settings updated", adminService.updateSettings(request, authentication));
    }
}
