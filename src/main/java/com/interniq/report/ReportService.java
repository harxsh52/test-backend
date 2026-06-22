package com.interniq.report;

import com.interniq.attendance.Attendance;
import com.interniq.attendance.AttendanceRepository;
import com.interniq.common.PageRequestFactory;
import com.interniq.common.PageResponse;
import com.interniq.common.PagingUtils;
import com.interniq.candidate.CandidateRepository;
import com.interniq.department.Department;
import com.interniq.department.DepartmentRepository;
import com.interniq.feedback.Feedback;
import com.interniq.feedback.FeedbackRepository;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.report.dto.AttendanceReportResponse;
import com.interniq.report.dto.DashboardStatsResponse;
import com.interniq.report.dto.DepartmentReportResponse;
import com.interniq.report.dto.InternReportResponse;
import com.interniq.report.dto.TaskReportResponse;
import com.interniq.task.Priority;
import com.interniq.task.Task;
import com.interniq.task.TaskRepository;
import com.interniq.task.TaskStatus;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final InternProfileRepository internProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final FeedbackRepository feedbackRepository;
    private final CandidateRepository candidateRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        return switch (currentUser.getRole()) {
            case INTERN -> buildInternDashboard(currentUser);
            case MANAGER -> buildManagerDashboard(currentUser);
            case HR -> buildHrDashboard();
            case ADMIN -> buildAdminDashboard();
        };
    }

    @Transactional(readOnly = true)
    public InternReportResponse getInternReport(Long internId, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        InternProfile intern = getAccessibleIntern(internId, currentUser);
        return buildInternReport(intern);
    }

    @Transactional(readOnly = true)
    public AttendanceReportResponse getAttendanceReport(
            Long internId,
            Long departmentId,
            LocalDate fromDate,
            LocalDate toDate,
            Authentication authentication
    ) {
        return getAttendanceReport(internId, departmentId, fromDate, toDate, null, null, "date", "DESC", authentication);
    }

    @Transactional(readOnly = true)
    public AttendanceReportResponse getAttendanceReport(
            Long internId,
            Long departmentId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        User currentUser = userService.getCurrentUser(authentication);
        Set<Long> accessibleInternIds = getAccessibleInterns(currentUser)
                .stream()
                .map(InternProfile::getId)
                .collect(Collectors.toSet());

        if (internId != null && !accessibleInternIds.contains(internId)) {
            throw new AccessDeniedException("You are not allowed to access this intern attendance report");
        }

        List<Attendance> records = attendanceRepository.findAll()
                .stream()
                .filter(attendance -> accessibleInternIds.contains(attendance.getIntern().getId()))
                .filter(attendance -> internId == null || Objects.equals(attendance.getIntern().getId(), internId))
                .filter(attendance -> departmentId == null
                        || (attendance.getIntern().getDepartment() != null
                        && Objects.equals(attendance.getIntern().getDepartment().getId(), departmentId)))
                .filter(attendance -> fromDate == null || !attendance.getDate().isBefore(fromDate))
                .filter(attendance -> toDate == null || !attendance.getDate().isAfter(toDate))
                .sorted(Comparator.comparing(Attendance::getDate).reversed())
                .toList();

        List<AttendanceReportResponse.Row> rows = records.stream().map(this::toAttendanceRow).toList();
        Page<AttendanceReportResponse.Row> pagedRows = PageRequestFactory.isPaged(page, size)
                ? PagingUtils.paginate(rows, reportPageable(page, size, sortBy, sortDirection, "date"))
                : null;

        return AttendanceReportResponse.builder()
                .totalRecords((long) records.size())
                .presentDays(countPresentDays(records))
                .totalWorkingHours(sumHours(records))
                .attendancePercentage(attendancePercentage(records))
                .records(pagedRows == null ? rows : pagedRows.getContent())
                .page(pagedRows == null ? null : pagedRows.getNumber())
                .size(pagedRows == null ? null : pagedRows.getSize())
                .totalElements(pagedRows == null ? (long) rows.size() : pagedRows.getTotalElements())
                .totalPages(pagedRows == null ? null : pagedRows.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public TaskReportResponse getTaskReport(
            Long internId,
            Long managerId,
            TaskStatus status,
            Priority priority,
            LocalDate fromDate,
            LocalDate toDate,
            Authentication authentication
    ) {
        return getTaskReport(internId, managerId, status, priority, fromDate, toDate, null, null, "createdAt", "DESC", authentication);
    }

    @Transactional(readOnly = true)
    public TaskReportResponse getTaskReport(
            Long internId,
            Long managerId,
            TaskStatus status,
            Priority priority,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        User currentUser = userService.getCurrentUser(authentication);
        Set<Long> accessibleInternIds = getAccessibleInterns(currentUser)
                .stream()
                .map(InternProfile::getId)
                .collect(Collectors.toSet());

        if (internId != null && !accessibleInternIds.contains(internId)) {
            throw new AccessDeniedException("You are not allowed to access this intern task report");
        }

        if (currentUser.getRole() == Role.MANAGER && managerId != null && !Objects.equals(managerId, currentUser.getId())) {
            throw new AccessDeniedException("Managers can filter only their own task reports");
        }

        List<Task> records = getAccessibleTasks(currentUser)
                .stream()
                .filter(task -> internId == null || Objects.equals(task.getAssignedTo().getId(), internId))
                .filter(task -> managerId == null || Objects.equals(task.getAssignedBy().getId(), managerId))
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> priority == null || task.getPriority() == priority)
                .filter(task -> fromDate == null || !task.getCreatedAt().toLocalDate().isBefore(fromDate))
                .filter(task -> toDate == null || !task.getCreatedAt().toLocalDate().isAfter(toDate))
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .toList();

        long completedTasks = countCompletedTasks(records);
        long pendingTasks = records.size() - completedTasks;
        long submittedTasks = records.stream().filter(task -> task.getStatus() == TaskStatus.SUBMITTED).count();

        List<TaskReportResponse.Row> rows = records.stream().map(this::toTaskRow).toList();
        Page<TaskReportResponse.Row> pagedRows = PageRequestFactory.isPaged(page, size)
                ? PagingUtils.paginate(rows, reportPageable(page, size, sortBy, sortDirection, "createdAt"))
                : null;

        return TaskReportResponse.builder()
                .totalTasks((long) records.size())
                .completedTasks(completedTasks)
                .pendingTasks(pendingTasks)
                .submittedTasks(submittedTasks)
                .completionPercentage(percentage(completedTasks, records.size()))
                .averageRating(averageTaskRating(records))
                .records(pagedRows == null ? rows : pagedRows.getContent())
                .page(pagedRows == null ? null : pagedRows.getNumber())
                .size(pagedRows == null ? null : pagedRows.getSize())
                .totalElements(pagedRows == null ? (long) rows.size() : pagedRows.getTotalElements())
                .totalPages(pagedRows == null ? null : pagedRows.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DepartmentReportResponse> getDepartmentReports(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        if (!isHrOrAdmin(currentUser)) {
            throw new AccessDeniedException("Only HR and admin users can view department reports");
        }

        return departmentRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::buildDepartmentReport)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentReportResponse> searchDepartmentReports(
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        Pageable pageable = reportPageable(page, size, sortBy, sortDirection, "departmentName");
        return PageResponse.from(PagingUtils.paginate(getDepartmentReports(authentication), pageable), sortBy, sortDirection);
    }

    private DashboardStatsResponse buildInternDashboard(User currentUser) {
        InternProfile profile = internProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));
        List<Task> tasks = tasksForIntern(profile);
        List<Attendance> attendance = attendanceForIntern(profile);
        BigDecimal averageRating = averageTaskRating(tasks);

        return DashboardStatsResponse.builder()
                .role(Role.INTERN)
                .totalTasks((long) tasks.size())
                .completedTasks(countCompletedTasks(tasks))
                .pendingTasks((long) tasks.size() - countCompletedTasks(tasks))
                .attendancePercentage(attendancePercentage(attendance))
                .totalWorkingHours(sumHours(attendance))
                .averageRating(averageRating)
                .experienceScore(experienceScore(attendancePercentage(attendance), completionPercentage(tasks), averageRating))
                .build();
    }

    private DashboardStatsResponse buildManagerDashboard(User manager) {
        List<InternProfile> interns = internProfileRepository.findByManager_Id(manager.getId());
        Set<Long> internIds = interns.stream().map(InternProfile::getId).collect(Collectors.toSet());
        List<Task> tasks = taskRepository.findAll()
                .stream()
                .filter(task -> internIds.contains(task.getAssignedTo().getId()) || Objects.equals(task.getAssignedBy().getId(), manager.getId()))
                .toList();

        return DashboardStatsResponse.builder()
                .role(Role.MANAGER)
                .totalInterns((long) interns.size())
                .activeInterns(interns.stream().filter(this::isActiveIntern).count())
                .tasksAssigned((long) tasks.size())
                .pendingReviews(tasks.stream().filter(task -> task.getStatus() == TaskStatus.SUBMITTED).count())
                .completedTasks(countCompletedTasks(tasks))
                .averageInternScore(averageInternScore(interns))
                .build();
    }

    private DashboardStatsResponse buildHrDashboard() {
        List<InternProfile> interns = internProfileRepository.findAll();
        List<Attendance> attendance = attendanceRepository.findAll();

        return DashboardStatsResponse.builder()
                .role(Role.HR)
                .totalCandidates(candidateRepository.count())
                .totalInterns((long) interns.size())
                .activeInterns(interns.stream().filter(this::isActiveIntern).count())
                .completedInternships(interns.stream().filter(intern -> "COMPLETED".equalsIgnoreCase(intern.getStatus())).count())
                .averageAttendance(attendancePercentage(attendance))
                .departmentsCount(departmentRepository.count())
                .build();
    }

    private DashboardStatsResponse buildAdminDashboard() {
        return DashboardStatsResponse.builder()
                .role(Role.ADMIN)
                .totalUsers(userRepository.count())
                .totalDepartments(departmentRepository.count())
                .totalInterns((long) userRepository.findByRoleOrderByNameAsc(Role.INTERN).size())
                .totalManagers((long) userRepository.findByRoleOrderByNameAsc(Role.MANAGER).size())
                .totalHR((long) userRepository.findByRoleOrderByNameAsc(Role.HR).size())
                .activeUsers(userRepository.findAll().stream().filter(User::isActive).count())
                .build();
    }

    private InternReportResponse buildInternReport(InternProfile intern) {
        List<Attendance> attendance = attendanceForIntern(intern);
        List<Task> tasks = tasksForIntern(intern);
        List<Feedback> feedback = feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(intern.getId());
        BigDecimal averageTaskRating = averageTaskRating(tasks);
        BigDecimal averageFeedbackRating = averageFeedbackRating(feedback);
        BigDecimal attendancePercentage = attendancePercentage(attendance);
        BigDecimal completionPercentage = completionPercentage(tasks);
        BigDecimal finalScore = experienceScore(attendancePercentage, completionPercentage, coalesceRating(averageTaskRating, averageFeedbackRating));

        return InternReportResponse.builder()
                .internId(intern.getId())
                .userId(intern.getUser().getId())
                .name(intern.getUser().getName())
                .email(intern.getUser().getEmail())
                .phone(intern.getPhone())
                .college(intern.getCollege())
                .skills(intern.getSkills())
                .status(intern.getStatus())
                .departmentId(intern.getDepartment() == null ? null : intern.getDepartment().getId())
                .departmentName(intern.getDepartment() == null ? null : intern.getDepartment().getName())
                .managerId(intern.getManager() == null ? null : intern.getManager().getId())
                .managerName(intern.getManager() == null ? null : intern.getManager().getName())
                .joiningDate(intern.getJoiningDate())
                .internshipStartDate(intern.getInternshipStartDate())
                .internshipEndDate(intern.getInternshipEndDate())
                .totalWorkingDays(countPresentDays(attendance))
                .totalWorkingHours(sumHours(attendance))
                .attendancePercentage(attendancePercentage)
                .tasksAssigned((long) tasks.size())
                .tasksCompleted(countCompletedTasks(tasks))
                .pendingTasks((long) tasks.size() - countCompletedTasks(tasks))
                .submittedTasks(tasks.stream().filter(task -> task.getStatus() == TaskStatus.SUBMITTED).count())
                .averageTaskRating(averageTaskRating)
                .feedbackCount((long) feedback.size())
                .averageFeedbackRating(averageFeedbackRating)
                .managerFeedback(feedback.stream().map(Feedback::getFeedbackText).toList())
                .finalExperienceScore(finalScore)
                .overallExperienceSummary(summaryForScore(finalScore))
                .build();
    }

    private DepartmentReportResponse buildDepartmentReport(Department department) {
        List<InternProfile> interns = internProfileRepository.findAll()
                .stream()
                .filter(intern -> intern.getDepartment() != null && Objects.equals(intern.getDepartment().getId(), department.getId()))
                .toList();
        List<Attendance> attendance = attendanceRepository.findAll()
                .stream()
                .filter(record -> record.getIntern().getDepartment() != null && Objects.equals(record.getIntern().getDepartment().getId(), department.getId()))
                .toList();
        List<Task> tasks = taskRepository.findAll()
                .stream()
                .filter(task -> task.getAssignedTo().getDepartment() != null && Objects.equals(task.getAssignedTo().getDepartment().getId(), department.getId()))
                .toList();

        return DepartmentReportResponse.builder()
                .departmentId(department.getId())
                .departmentName(department.getName())
                .totalInterns((long) interns.size())
                .activeInterns(interns.stream().filter(this::isActiveIntern).count())
                .averageAttendance(attendancePercentage(attendance))
                .averageTaskCompletion(completionPercentage(tasks))
                .averageRating(averageTaskRating(tasks))
                .build();
    }

    private List<InternProfile> getAccessibleInterns(User user) {
        return switch (user.getRole()) {
            case ADMIN, HR -> internProfileRepository.findAll();
            case MANAGER -> internProfileRepository.findByManager_Id(user.getId());
            case INTERN -> internProfileRepository.findByUserId(user.getId()).map(List::of).orElseGet(List::of);
        };
    }

    private InternProfile getAccessibleIntern(Long internId, User currentUser) {
        return getAccessibleInterns(currentUser)
                .stream()
                .filter(intern -> Objects.equals(intern.getId(), internId))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to access this intern report"));
    }

    private List<Task> getAccessibleTasks(User currentUser) {
        if (isHrOrAdmin(currentUser)) {
            return taskRepository.findAll();
        }

        if (currentUser.getRole() == Role.INTERN) {
            return taskRepository.findByAssignedTo_User_IdOrderByCreatedAtDesc(currentUser.getId());
        }

        Set<Long> managerInternIds = internProfileRepository.findByManager_Id(currentUser.getId())
                .stream()
                .map(InternProfile::getId)
                .collect(Collectors.toSet());

        return taskRepository.findAll()
                .stream()
                .filter(task -> managerInternIds.contains(task.getAssignedTo().getId()) || Objects.equals(task.getAssignedBy().getId(), currentUser.getId()))
                .toList();
    }

    private List<Attendance> attendanceForIntern(InternProfile intern) {
        return attendanceRepository.findByIntern_IdOrderByDateDesc(intern.getId());
    }

    private List<Task> tasksForIntern(InternProfile intern) {
        return taskRepository.findAll()
                .stream()
                .filter(task -> Objects.equals(task.getAssignedTo().getId(), intern.getId()))
                .toList();
    }

    private AttendanceReportResponse.Row toAttendanceRow(Attendance attendance) {
        return AttendanceReportResponse.Row.builder()
                .id(attendance.getId())
                .internId(attendance.getIntern().getId())
                .internName(attendance.getIntern().getUser().getName())
                .departmentId(attendance.getIntern().getDepartment() == null ? null : attendance.getIntern().getDepartment().getId())
                .departmentName(attendance.getIntern().getDepartment() == null ? null : attendance.getIntern().getDepartment().getName())
                .date(attendance.getDate())
                .punchInTime(attendance.getPunchInTime())
                .punchOutTime(attendance.getPunchOutTime())
                .totalHours(attendance.getTotalHours())
                .status(attendance.getStatus())
                .build();
    }

    private TaskReportResponse.Row toTaskRow(Task task) {
        return TaskReportResponse.Row.builder()
                .id(task.getId())
                .title(task.getTitle())
                .internId(task.getAssignedTo().getId())
                .internName(task.getAssignedTo().getUser().getName())
                .managerId(task.getAssignedBy().getId())
                .managerName(task.getAssignedBy().getName())
                .priority(task.getPriority())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .rating(task.getRating())
                .managerFeedback(task.getManagerFeedback())
                .createdAt(task.getCreatedAt())
                .submittedAt(task.getSubmittedAt())
                .reviewedAt(task.getReviewedAt())
                .build();
    }

    private long countPresentDays(List<Attendance> attendance) {
        return attendance.stream()
                .filter(record -> "PRESENT".equalsIgnoreCase(record.getStatus()) || "PUNCHED_IN".equalsIgnoreCase(record.getStatus()))
                .count();
    }

    private BigDecimal sumHours(List<Attendance> attendance) {
        return attendance.stream()
                .map(Attendance::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal attendancePercentage(List<Attendance> attendance) {
        if (attendance.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return percentage(countPresentDays(attendance), attendance.size());
    }

    private long countCompletedTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.APPROVED || task.getStatus() == TaskStatus.COMPLETED)
                .count();
    }

    private BigDecimal completionPercentage(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return percentage(countCompletedTasks(tasks), tasks.size());
    }

    private BigDecimal averageTaskRating(List<Task> tasks) {
        return average(tasks.stream()
                .map(Task::getRating)
                .filter(Objects::nonNull)
                .toList());
    }

    private BigDecimal averageFeedbackRating(List<Feedback> feedback) {
        return average(feedback.stream()
                .map(Feedback::getRating)
                .filter(Objects::nonNull)
                .toList());
    }

    private BigDecimal average(List<Integer> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(values.stream().mapToInt(Integer::intValue).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageInternScore(List<InternProfile> interns) {
        if (interns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalScore = interns.stream()
                .map(this::buildInternReport)
                .map(InternReportResponse::getFinalExperienceScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalScore.divide(BigDecimal.valueOf(interns.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal coalesceRating(BigDecimal primary, BigDecimal fallback) {
        return primary != null && primary.compareTo(BigDecimal.ZERO) > 0 ? primary : fallback;
    }

    private BigDecimal experienceScore(BigDecimal attendancePercentage, BigDecimal completionPercentage, BigDecimal averageRating) {
        BigDecimal ratingScore = averageRating == null ? BigDecimal.ZERO : averageRating.multiply(BigDecimal.valueOf(20));
        return attendancePercentage.multiply(BigDecimal.valueOf(0.35))
                .add(completionPercentage.multiply(BigDecimal.valueOf(0.4)))
                .add(ratingScore.multiply(BigDecimal.valueOf(0.25)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private boolean isActiveIntern(InternProfile intern) {
        return "ACTIVE".equalsIgnoreCase(intern.getStatus());
    }

    private boolean isHrOrAdmin(User user) {
        return user.getRole() == Role.HR || user.getRole() == Role.ADMIN;
    }

    private String summaryForScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "Excellent internship performance with strong attendance, task completion, and manager feedback.";
        }

        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "Good internship progress with room to improve consistency, delivery, or feedback outcomes.";
        }

        return "Needs focused improvement across attendance, task progress, and feedback quality.";
    }

    private Pageable reportPageable(Integer page, Integer size, String sortBy, String sortDirection, String defaultSortBy) {
        return PageRequestFactory.create(
                page,
                size,
                sortBy,
                sortDirection,
                Set.of("id", "date", "createdAt", "departmentName", "status", "priority", "finalScore"),
                defaultSortBy
        );
    }
}
