package com.interniq.admin;

import com.interniq.admin.dto.AdminDtos.*;
import com.interniq.attendance.Attendance;
import com.interniq.attendance.AttendanceRepository;
import com.interniq.audit.AuditLog;
import com.interniq.audit.AuditLogRepository;
import com.interniq.audit.AuditLogService;
import com.interniq.audit.LoginAuditService;
import com.interniq.audit.dto.LoginAuditLogResponse;
import com.interniq.candidate.Candidate;
import com.interniq.candidate.CandidateRepository;
import com.interniq.candidate.CandidateStatus;
import com.interniq.department.Department;
import com.interniq.department.DepartmentRepository;
import com.interniq.feedback.Feedback;
import com.interniq.feedback.FeedbackRepository;
import com.interniq.feedback.InternManagerFeedback;
import com.interniq.feedback.InternManagerFeedbackRepository;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.interview.Interview;
import com.interniq.interview.InterviewRepository;
import com.interniq.interview.InterviewResult;
import com.interniq.interview.InterviewStatus;
import com.interniq.notification.EmailNotificationRepository;
import com.interniq.task.Priority;
import com.interniq.task.Task;
import com.interniq.task.TaskRepository;
import com.interniq.task.TaskStatus;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%".toCharArray();

    private final UserService userService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final AssignedCompanyRepository assignedCompanyRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final InternProfileRepository internProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final FeedbackRepository feedbackRepository;
    private final InternManagerFeedbackRepository internManagerFeedbackRepository;
    private final CandidateRepository candidateRepository;
    private final InterviewRepository interviewRepository;
    private final EmailNotificationRepository emailNotificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final LoginAuditService loginAuditService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard(Authentication auth) {
        admin(auth);
        List<InternProfile> interns = internProfileRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        List<Attendance> attendance = attendanceRepository.findAll();
        List<Feedback> feedback = feedbackRepository.findAll();
        List<InternScoreResponse> scores = interns.stream()
                .map(intern -> internScore(intern, attendanceFor(attendance, intern), tasksFor(tasks, intern), feedbackFor(feedback, intern), null))
                .toList();

        return AdminDashboardResponse.builder()
                .summaryCards(summaryCards(interns, tasks, attendance, scores))
                .analytics(Map.of(
                        "telecomVsWealthPerformance", groupAverage(scores, InternScoreResponse::getDepartment),
                        "departmentWiseInterns", countBy(interns.stream().map(this::departmentName)),
                        "companyWiseInterns", countBy(interns.stream().map(i -> firstNonBlank(i.getAssignedCompany(), "Unassigned"))),
                        "taskCompletionSummary", taskSummary(tasks),
                        "attendanceSummary", attendanceSummary(attendance),
                        "managerPerformanceSummary", managerPerformanceRows(interns, tasks, feedback),
                        "candidatePipelineSummary", countBy(candidateRepository.findAll().stream().map(c -> c.getStatus().name())),
                        "interviewStatusSummary", countBy(interviewRepository.findAll().stream().map(i -> i.getStatus().name())),
                        "monthlyOnboardingSummary", countBy(interns.stream().map(i -> i.getJoiningDate() == null ? "Unscheduled" : i.getJoiningDate().getYear() + "-" + "%02d".formatted(i.getJoiningDate().getMonthValue())))
                ))
                .topInterns(scores.stream().sorted(Comparator.comparing(InternScoreResponse::getFinalScore).reversed()).limit(5).toList())
                .lowPerformingInterns(scores.stream().sorted(Comparator.comparing(InternScoreResponse::getFinalScore)).limit(5).toList())
                .managerPerformance(managerPerformanceRows(interns, tasks, feedback))
                .recentActivity(recentActivity())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> users(Role role, String status, String search, Authentication auth) {
        admin(auth);
        return userRepository.findAllByOrderByNameAsc().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> status == null || status.isBlank() || status.equalsIgnoreCase(u.getStatus()) || (status.equalsIgnoreCase("ACTIVE") == u.isActive()))
                .filter(u -> contains(search, u.getName(), u.getEmail(), u.getDepartment(), u.getEmpId()))
                .map(this::user)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserAdminResponse user(Long userId, Authentication auth) {
        admin(auth);
        return user(userOrThrow(userId));
    }

    @Transactional
    public UserAdminResponse createUser(UserAdminRequest req, Authentication auth) {
        User actor = admin(auth);
        String email = clean(req.getEmail()).toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User saved = userRepository.save(User.builder()
                .name(clean(req.getName()))
                .email(email)
                .phone(clean(req.getPhone()))
                .empId(clean(req.getEmpId()))
                .profileImageUrl(clean(req.getProfileImageUrl()))
                .password(passwordEncoder.encode(firstNonBlank(req.getPassword(), generateTemporaryPassword())))
                .role(req.getRole())
                .department(clean(req.getDepartment()))
                .managerName(clean(req.getManagerName()))
                .active(isActive(req.getStatus()))
                .status(status(req.getStatus()))
                .build());
        auditLogService.record(actor, "ADMIN_USER_CREATED", "User", saved.getId());
        return user(saved);
    }

    @Transactional
    public UserAdminResponse updateUser(Long userId, UserAdminRequest req, Authentication auth) {
        User actor = admin(auth);
        User target = userOrThrow(userId);
        String email = clean(req.getEmail()).toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> { throw new IllegalArgumentException("Email is already registered"); });
        target.setName(clean(req.getName()));
        target.setEmail(email);
        target.setPhone(clean(req.getPhone()));
        target.setEmpId(clean(req.getEmpId()));
        target.setProfileImageUrl(clean(req.getProfileImageUrl()));
        target.setRole(req.getRole());
        target.setDepartment(clean(req.getDepartment()));
        target.setManagerName(clean(req.getManagerName()));
        target.setStatus(status(req.getStatus()));
        target.setActive(isActive(req.getStatus()));
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            target.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        auditLogService.record(actor, "ADMIN_USER_UPDATED", "User", target.getId());
        return user(target);
    }

    @Transactional
    public UserAdminResponse updateUserStatus(Long userId, StatusRequest req, Authentication auth) {
        User actor = admin(auth);
        User target = userOrThrow(userId);
        target.setStatus(status(req.getStatus()));
        target.setActive(isActive(req.getStatus()));
        auditLogService.record(actor, "ADMIN_USER_STATUS_CHANGED", "User", target.getId());
        return user(target);
    }

    @Transactional
    public UserAdminResponse updateUserRole(Long userId, RoleRequest req, Authentication auth) {
        User actor = admin(auth);
        User target = userOrThrow(userId);
        target.setRole(req.getRole());
        auditLogService.record(actor, "ADMIN_USER_ROLE_CHANGED", "User", target.getId());
        return user(target);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long userId, ResetPasswordRequest req, Authentication auth) {
        User actor = admin(auth);
        User target = userOrThrow(userId);
        String temporaryPassword = firstNonBlank(req.getPassword(), generateTemporaryPassword());
        target.setPassword(passwordEncoder.encode(temporaryPassword));
        auditLogService.record(actor, "ADMIN_PASSWORD_RESET", "User", target.getId());
        return ResetPasswordResponse.builder().userId(target.getId()).email(target.getEmail()).temporaryPassword(temporaryPassword).build();
    }

    @Transactional
    public UserAdminResponse lockUser(Long userId, Authentication auth) {
        User actor = admin(auth);
        User target = userOrThrow(userId);
        target.setAccountLocked(true);
        target.setStatus("LOCKED");
        auditLogService.record(actor, "ADMIN_USER_LOCKED", "User", target.getId());
        return user(target);
    }

    @Transactional
    public UserAdminResponse unlockUser(Long userId, Authentication auth) {
        User actor = admin(auth);
        User target = userOrThrow(userId);
        target.setAccountLocked(false);
        target.setFailedLoginAttempts(0);
        target.setActive(true);
        target.setStatus("ACTIVE");
        auditLogService.record(actor, "ADMIN_USER_UNLOCKED", "User", target.getId());
        return user(target);
    }

    @Transactional(readOnly = true)
    public List<LoginAuditLogResponse> loginAuditLogs(Authentication auth) {
        admin(auth);
        return loginAuditService.latest();
    }

    @Transactional(readOnly = true)
    public List<InternAdminResponse> interns(Long departmentId, Long managerId, String status, String search, Authentication auth) {
        admin(auth);
        return internProfileRepository.findAll().stream()
                .filter(i -> departmentId == null || (i.getDepartment() != null && Objects.equals(i.getDepartment().getId(), departmentId)))
                .filter(i -> managerId == null || (i.getManager() != null && Objects.equals(i.getManager().getId(), managerId)))
                .filter(i -> status == null || status.isBlank() || status.equalsIgnoreCase(i.getStatus()))
                .filter(i -> contains(search, i.getUser().getName(), i.getUser().getEmail(), i.getEmpId(), departmentName(i), i.getAssignedCompany()))
                .sorted(Comparator.comparing(i -> i.getUser().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::intern)
                .toList();
    }

    @Transactional(readOnly = true)
    public InternAdminResponse intern(Long internId, Authentication auth) {
        admin(auth);
        return intern(profileOrThrow(internId));
    }

    @Transactional
    public InternAdminResponse updateIntern(Long internId, InternAdminRequest req, Authentication auth) {
        User actor = admin(auth);
        InternProfile profile = profileOrThrow(internId);
        applyIntern(profile, req);
        auditLogService.record(actor, "ADMIN_INTERN_UPDATED", "InternProfile", profile.getId());
        return intern(profile);
    }

    @Transactional
    public InternAdminResponse assignManager(Long internId, AssignManagerRequest req, Authentication auth) {
        User actor = admin(auth);
        InternProfile profile = profileOrThrow(internId);
        User manager = userOrThrow(req.getManagerId());
        ensureRole(manager, Role.MANAGER, "Selected user must be a manager");
        profile.setManager(manager);
        profile.getUser().setManagerName(manager.getName());
        auditLogService.record(actor, "ADMIN_MANAGER_ASSIGNED", "InternProfile", profile.getId());
        return intern(profile);
    }

    @Transactional
    public InternAdminResponse assignDepartment(Long internId, AssignDepartmentRequest req, Authentication auth) {
        User actor = admin(auth);
        InternProfile profile = profileOrThrow(internId);
        Department department = departmentOrThrow(req.getDepartmentId());
        profile.setDepartment(department);
        profile.setSubDepartment(clean(req.getSubDepartment()));
        profile.setAssignedCompany(clean(req.getAssignedCompany()));
        profile.getUser().setDepartment(department.getName());
        auditLogService.record(actor, "ADMIN_DEPARTMENT_ASSIGNED", "InternProfile", profile.getId());
        return intern(profile);
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> managers(Authentication auth) {
        admin(auth);
        return users(Role.MANAGER, null, null, auth);
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> hrUsers(Authentication auth) {
        admin(auth);
        return users(Role.HR, null, null, auth);
    }

    @Transactional(readOnly = true)
    public List<InternAdminResponse> managerInterns(Long managerId, Authentication auth) {
        admin(auth);
        ensureRole(userOrThrow(managerId), Role.MANAGER, "Manager not found");
        return interns(null, managerId, null, null, auth);
    }

    @Transactional(readOnly = true)
    public List<DepartmentAdminResponse> departments(Authentication auth) {
        admin(auth);
        List<InternProfile> interns = internProfileRepository.findAll();
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(department -> DepartmentAdminResponse.builder()
                        .id(department.getId())
                        .name(department.getName())
                        .description(department.getDescription())
                        .status(department.isActive() ? "ACTIVE" : "INACTIVE")
                        .totalInterns(interns.stream().filter(i -> i.getDepartment() != null && Objects.equals(i.getDepartment().getId(), department.getId())).count())
                        .activeInterns(interns.stream().filter(i -> i.getDepartment() != null && Objects.equals(i.getDepartment().getId(), department.getId()) && "ACTIVE".equalsIgnoreCase(i.getStatus())).count())
                        .build())
                .toList();
    }

    @Transactional
    public DepartmentAdminResponse createDepartment(DepartmentAdminRequest req, Authentication auth) {
        User actor = admin(auth);
        String name = clean(req.getName());
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Department already exists");
        }
        Department saved = departmentRepository.save(Department.builder().name(name).description(clean(req.getDescription())).active(isActive(req.getStatus())).build());
        auditLogService.record(actor, "ADMIN_DEPARTMENT_CREATED", "Department", saved.getId());
        return departments(auth).stream().filter(d -> d.getId().equals(saved.getId())).findFirst().orElseThrow();
    }

    @Transactional
    public DepartmentAdminResponse updateDepartment(Long id, DepartmentAdminRequest req, Authentication auth) {
        User actor = admin(auth);
        Department department = departmentOrThrow(id);
        departmentRepository.findByNameIgnoreCase(clean(req.getName())).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new IllegalArgumentException("Department already exists");
        });
        department.setName(clean(req.getName()));
        department.setDescription(clean(req.getDescription()));
        department.setActive(isActive(req.getStatus()));
        auditLogService.record(actor, "ADMIN_DEPARTMENT_UPDATED", "Department", department.getId());
        return departments(auth).stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    public DepartmentAdminResponse deleteDepartment(Long id, Authentication auth) {
        User actor = admin(auth);
        Department department = departmentOrThrow(id);
        department.setActive(false);
        auditLogService.record(actor, "ADMIN_DEPARTMENT_DISABLED", "Department", department.getId());
        return departments(auth).stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<CatalogResponse> subDepartments(Authentication auth) {
        admin(auth);
        return subDepartmentRepository.findAllByOrderByDepartment_NameAscNameAsc().stream().map(this::catalog).toList();
    }

    @Transactional
    public CatalogResponse createSubDepartment(CatalogRequest req, Authentication auth) {
        User actor = admin(auth);
        Department department = departmentOrThrow(req.getDepartmentId());
        if (subDepartmentRepository.existsByDepartment_IdAndNameIgnoreCase(department.getId(), clean(req.getName()))) {
            throw new IllegalArgumentException("Sub department already exists");
        }
        SubDepartment saved = subDepartmentRepository.save(SubDepartment.builder().department(department).name(clean(req.getName())).description(clean(req.getDescription())).status(status(req.getStatus())).build());
        auditLogService.record(actor, "ADMIN_SUB_DEPARTMENT_CREATED", "SubDepartment", saved.getId());
        return catalog(saved);
    }

    @Transactional
    public CatalogResponse updateSubDepartment(Long id, CatalogRequest req, Authentication auth) {
        User actor = admin(auth);
        SubDepartment item = subDepartmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Sub department not found"));
        item.setDepartment(departmentOrThrow(req.getDepartmentId()));
        item.setName(clean(req.getName()));
        item.setDescription(clean(req.getDescription()));
        item.setStatus(status(req.getStatus()));
        auditLogService.record(actor, "ADMIN_SUB_DEPARTMENT_UPDATED", "SubDepartment", item.getId());
        return catalog(item);
    }

    @Transactional
    public CatalogResponse deleteSubDepartment(Long id, Authentication auth) {
        User actor = admin(auth);
        SubDepartment item = subDepartmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Sub department not found"));
        item.setStatus("INACTIVE");
        auditLogService.record(actor, "ADMIN_SUB_DEPARTMENT_DISABLED", "SubDepartment", item.getId());
        return catalog(item);
    }

    @Transactional(readOnly = true)
    public List<CatalogResponse> assignedCompanies(Authentication auth) {
        admin(auth);
        return assignedCompanyRepository.findAllByOrderByDepartment_NameAscNameAsc().stream().map(this::catalog).toList();
    }

    @Transactional
    public CatalogResponse createAssignedCompany(CatalogRequest req, Authentication auth) {
        User actor = admin(auth);
        Department department = departmentOrThrow(req.getDepartmentId());
        if (assignedCompanyRepository.existsByDepartment_IdAndNameIgnoreCase(department.getId(), clean(req.getName()))) {
            throw new IllegalArgumentException("Assigned company already exists");
        }
        AssignedCompany saved = assignedCompanyRepository.save(AssignedCompany.builder().department(department).name(clean(req.getName())).description(clean(req.getDescription())).status(status(req.getStatus())).build());
        auditLogService.record(actor, "ADMIN_ASSIGNED_COMPANY_CREATED", "AssignedCompany", saved.getId());
        return catalog(saved);
    }

    @Transactional
    public CatalogResponse updateAssignedCompany(Long id, CatalogRequest req, Authentication auth) {
        User actor = admin(auth);
        AssignedCompany item = assignedCompanyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Assigned company not found"));
        item.setDepartment(departmentOrThrow(req.getDepartmentId()));
        item.setName(clean(req.getName()));
        item.setDescription(clean(req.getDescription()));
        item.setStatus(status(req.getStatus()));
        auditLogService.record(actor, "ADMIN_ASSIGNED_COMPANY_UPDATED", "AssignedCompany", item.getId());
        return catalog(item);
    }

    @Transactional
    public CatalogResponse deleteAssignedCompany(Long id, Authentication auth) {
        User actor = admin(auth);
        AssignedCompany item = assignedCompanyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Assigned company not found"));
        item.setStatus("INACTIVE");
        auditLogService.record(actor, "ADMIN_ASSIGNED_COMPANY_DISABLED", "AssignedCompany", item.getId());
        return catalog(item);
    }

    @Transactional(readOnly = true)
    public List<AttendanceAdminResponse> attendance(Long internId, Long departmentId, LocalDate fromDate, LocalDate toDate, String status, Authentication auth) {
        admin(auth);
        return attendanceRepository.findAll().stream()
                .filter(a -> internId == null || Objects.equals(a.getIntern().getId(), internId))
                .filter(a -> departmentId == null || (a.getIntern().getDepartment() != null && Objects.equals(a.getIntern().getDepartment().getId(), departmentId)))
                .filter(a -> fromDate == null || !a.getDate().isBefore(fromDate))
                .filter(a -> toDate == null || !a.getDate().isAfter(toDate))
                .filter(a -> status == null || status.isBlank() || status.equalsIgnoreCase(a.getStatus()))
                .sorted(Comparator.comparing(Attendance::getDate).reversed())
                .map(this::attendance)
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryResponse attendanceSummary(Authentication auth) {
        admin(auth);
        List<Attendance> records = attendanceRepository.findAll();
        return SummaryResponse.builder().metrics(attendanceSummary(records)).build();
    }

    @Transactional
    public SummaryResponse syncAttendance(Authentication auth) {
        User actor = admin(auth);
        auditLogService.record(actor, "ADMIN_ATTENDANCE_SYNC_TRIGGERED", "Attendance", null);
        return attendanceSummary(auth);
    }

    @Transactional(readOnly = true)
    public List<TaskAdminResponse> tasks(TaskStatus status, Priority priority, Long internId, Long managerId, Authentication auth) {
        admin(auth);
        return taskRepository.findAll().stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> internId == null || Objects.equals(t.getAssignedTo().getId(), internId))
                .filter(t -> managerId == null || Objects.equals(t.getAssignedBy().getId(), managerId))
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .map(this::task)
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryResponse tasksSummary(Authentication auth) {
        admin(auth);
        return SummaryResponse.builder().metrics(taskSummary(taskRepository.findAll())).build();
    }

    @Transactional(readOnly = true)
    public SummaryResponse feedbackSummary(Authentication auth) {
        admin(auth);
        List<Feedback> feedback = feedbackRepository.findAll();
        return SummaryResponse.builder().metrics(Map.of(
                "totalFeedback", feedback.size(),
                "averageRating", averageRating(feedback),
                "highRated", feedback.stream().filter(f -> f.getRating() != null && f.getRating() >= 4).count(),
                "needsImprovement", feedback.stream().filter(f -> f.getRating() != null && f.getRating() <= 2).count()
        )).build();
    }

    @Transactional(readOnly = true)
    public List<FeedbackAdminResponse> managerFeedback(Authentication auth) {
        admin(auth);
        return feedbackRepository.findAll().stream().sorted(Comparator.comparing(Feedback::getCreatedAt).reversed()).map(this::feedback).toList();
    }

    @Transactional(readOnly = true)
    public List<InternManagerFeedback> internManagerFeedback(Authentication auth) {
        admin(auth);
        return internManagerFeedbackRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Candidate> candidates(CandidateStatus status, String role, Authentication auth) {
        admin(auth);
        return candidateRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> role == null || role.isBlank() || c.getAppliedRole().toLowerCase(Locale.ROOT).contains(role.toLowerCase(Locale.ROOT)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryResponse candidatesSummary(Authentication auth) {
        admin(auth);
        List<Candidate> candidates = candidateRepository.findAll();
        return SummaryResponse.builder().metrics(Map.of(
                "totalCandidates", candidates.size(),
                "pipeline", countBy(candidates.stream().map(c -> c.getStatus().name())),
                "screened", candidates.stream().filter(c -> c.getStatus() == CandidateStatus.SCREENED).count(),
                "shortlisted", candidates.stream().filter(c -> c.getStatus() == CandidateStatus.SHORTLISTED).count()
        )).build();
    }

    @Transactional(readOnly = true)
    public List<InterviewAdminResponse> interviews(Authentication auth) {
        admin(auth);
        return interviewRepository.findAllByOrderByScheduledAtDesc().stream().map(this::interview).toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResultAdminResponse> interviewResults(Authentication auth) {
        admin(auth);
        return interviewRepository.findAllByOrderByScheduledAtDesc().stream()
                .map(Interview::getResult)
                .filter(Objects::nonNull)
                .map(this::interviewResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryResponse interviewsSummary(Authentication auth) {
        admin(auth);
        List<Interview> interviews = interviewRepository.findAll();
        return SummaryResponse.builder().metrics(Map.of(
                "totalInterviews", interviews.size(),
                "completed", interviews.stream().filter(i -> i.getStatus() == InterviewStatus.COMPLETED).count(),
                "scheduled", interviews.stream().filter(i -> i.getStatus() == InterviewStatus.SCHEDULED).count(),
                "averageScore", BigDecimal.valueOf(interviews.stream().filter(i -> i.getFinalScore() != null).mapToInt(Interview::getFinalScore).average().orElse(0)).setScale(1, RoundingMode.HALF_UP),
                "byStatus", countBy(interviews.stream().map(i -> i.getStatus().name()))
        )).build();
    }

    @Transactional(readOnly = true)
    public SummaryResponse reportsSummary(Authentication auth) {
        return SummaryResponse.builder().metrics(Map.of(
                "dashboard", dashboard(auth).getSummaryCards(),
                "attendance", attendanceSummary(auth).getMetrics(),
                "tasks", tasksSummary(auth).getMetrics(),
                "feedback", feedbackSummary(auth).getMetrics(),
                "candidates", candidatesSummary(auth).getMetrics(),
                "interviews", interviewsSummary(auth).getMetrics()
        )).build();
    }

    @Transactional(readOnly = true)
    public List<SearchResultResponse> search(String q, String type, Authentication auth) {
        admin(auth);
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Stream<SearchResultResponse> users = userRepository.findAll().stream()
                .filter(u -> type == null || type.isBlank() || "users".equalsIgnoreCase(type))
                .filter(u -> contains(query, u.getName(), u.getEmail(), u.getRole().name()))
                .map(u -> SearchResultResponse.builder().type("USER").id(u.getId()).title(u.getName()).subtitle(u.getEmail()).status(u.getStatus()).build());
        Stream<SearchResultResponse> interns = internProfileRepository.findAll().stream()
                .filter(i -> type == null || type.isBlank() || "interns".equalsIgnoreCase(type))
                .filter(i -> contains(query, i.getUser().getName(), i.getUser().getEmail(), i.getEmpId(), departmentName(i)))
                .map(i -> SearchResultResponse.builder().type("INTERN").id(i.getId()).title(i.getUser().getName()).subtitle(departmentName(i)).status(i.getStatus()).build());
        Stream<SearchResultResponse> candidates = candidateRepository.findAll().stream()
                .filter(c -> type == null || type.isBlank() || "candidates".equalsIgnoreCase(type))
                .filter(c -> contains(query, c.getName(), c.getEmail(), c.getAppliedRole()))
                .map(c -> SearchResultResponse.builder().type("CANDIDATE").id(c.getId()).title(c.getName()).subtitle(c.getAppliedRole()).status(c.getStatus().name()).build());
        return Stream.concat(Stream.concat(users, interns), candidates).limit(25).toList();
    }

    @Transactional(readOnly = true)
    public List<SettingResponse> settings(Authentication auth) {
        admin(auth);
        seedSettingsIfMissing();
        return systemSettingRepository.findAll().stream()
                .sorted(Comparator.comparing(SystemSetting::getKey))
                .map(this::setting)
                .toList();
    }

    @Transactional
    public List<SettingResponse> updateSettings(List<SettingRequest> requests, Authentication auth) {
        User actor = admin(auth);
        requests.forEach(req -> {
            SystemSetting setting = systemSettingRepository.findById(clean(req.getKey()))
                    .orElseGet(() -> SystemSetting.builder().key(clean(req.getKey())).build());
            setting.setValue(clean(req.getValue()));
            setting.setDescription(clean(req.getDescription()));
            systemSettingRepository.save(setting);
        });
        auditLogService.record(actor, "ADMIN_SETTINGS_UPDATED", "SystemSetting", null);
        return settings(auth);
    }

    private User admin(Authentication auth) {
        User user = userService.getCurrentUser(auth);
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can access this resource");
        }
        return user;
    }

    private User userOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private InternProfile profileOrThrow(Long id) {
        return internProfileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));
    }

    private Department departmentOrThrow(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    private void ensureRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new IllegalArgumentException(message);
        }
    }

    private void applyIntern(InternProfile profile, InternAdminRequest req) {
        if (req.getDepartmentId() != null) {
            Department department = departmentOrThrow(req.getDepartmentId());
            profile.setDepartment(department);
            profile.getUser().setDepartment(department.getName());
        }
        if (req.getManagerId() != null) {
            User manager = userOrThrow(req.getManagerId());
            ensureRole(manager, Role.MANAGER, "Selected user must be a manager");
            profile.setManager(manager);
            profile.getUser().setManagerName(manager.getName());
        }
        profile.setPhone(clean(req.getPhone()));
        profile.setEmpId(clean(req.getEmpId()));
        profile.setSubDepartment(clean(req.getSubDepartment()));
        profile.setAssignedCompany(clean(req.getAssignedCompany()));
        profile.setCollege(clean(req.getCollege()));
        profile.setSkills(clean(req.getSkills()));
        profile.setJoiningDate(req.getJoiningDate());
        profile.setInternshipStartDate(req.getInternshipStartDate());
        profile.setInternshipEndDate(req.getInternshipEndDate());
        profile.setStatus(status(req.getStatus()));
    }

    private UserAdminResponse user(User u) {
        return UserAdminResponse.builder()
                .id(u.getId()).name(u.getName()).email(u.getEmail()).phone(u.getPhone()).empId(u.getEmpId())
                .profileImageUrl(u.getProfileImageUrl()).role(u.getRole()).department(u.getDepartment())
                .managerName(u.getManagerName()).designation(designation(u.getRole())).status(u.getStatus())
                .active(u.isActive()).accountLocked(u.isAccountLocked()).failedLoginAttempts(u.getFailedLoginAttempts())
                .lastLoginAt(u.getLastLoginAt()).createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt()).build();
    }

    private InternAdminResponse intern(InternProfile i) {
        List<Attendance> attendance = attendanceRepository.findByIntern_IdOrderByDateDesc(i.getId());
        List<Task> tasks = taskRepository.findByAssignedTo_IdOrderByCreatedAtDesc(i.getId());
        List<Feedback> feedback = feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(i.getId());
        InternScoreResponse score = internScore(i, attendance, tasks, feedback, null);
        return InternAdminResponse.builder()
                .id(i.getId()).userId(i.getUser().getId()).empId(firstNonBlank(i.getEmpId(), i.getUser().getEmpId()))
                .name(i.getUser().getName()).email(i.getUser().getEmail()).phone(firstNonBlank(i.getPhone(), i.getUser().getPhone()))
                .departmentId(i.getDepartment() == null ? null : i.getDepartment().getId()).departmentName(departmentName(i))
                .subDepartment(i.getSubDepartment()).assignedCompany(i.getAssignedCompany())
                .managerId(i.getManager() == null ? null : i.getManager().getId()).managerName(i.getManager() == null ? null : i.getManager().getName())
                .college(i.getCollege()).skills(i.getSkills()).joiningDate(i.getJoiningDate()).internshipStartDate(i.getInternshipStartDate())
                .internshipEndDate(i.getInternshipEndDate()).status(i.getStatus()).attendancePercentage(score.getAttendancePercentage())
                .taskCompletionPercentage(score.getTaskCompletionPercentage()).averageRating(averageRating(feedback)).finalScore(score.getFinalScore()).build();
    }

    private CatalogResponse catalog(SubDepartment item) {
        return CatalogResponse.builder().id(item.getId()).departmentId(item.getDepartment().getId()).departmentName(item.getDepartment().getName())
                .name(item.getName()).description(item.getDescription()).status(item.getStatus()).createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt()).build();
    }

    private CatalogResponse catalog(AssignedCompany item) {
        return CatalogResponse.builder().id(item.getId()).departmentId(item.getDepartment().getId()).departmentName(item.getDepartment().getName())
                .name(item.getName()).description(item.getDescription()).status(item.getStatus()).createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt()).build();
    }

    private AttendanceAdminResponse attendance(Attendance a) {
        return AttendanceAdminResponse.builder().id(a.getId()).internId(a.getIntern().getId()).internName(a.getIntern().getUser().getName())
                .department(departmentName(a.getIntern())).date(a.getDate()).punchInTime(a.getPunchInTime()).punchOutTime(a.getPunchOutTime())
                .totalHours(a.getTotalHours()).status(a.getStatus()).source(a.getSource()).build();
    }

    private TaskAdminResponse task(Task t) {
        return TaskAdminResponse.builder().id(t.getId()).title(t.getTitle()).description(t.getDescription()).assignedToInternId(t.getAssignedTo().getId())
                .assignedToName(t.getAssignedTo().getUser().getName()).assignedById(t.getAssignedBy().getId()).assignedByName(t.getAssignedBy().getName())
                .priority(t.getPriority()).status(t.getStatus()).dueDate(t.getDueDate()).rating(t.getRating()).createdAt(t.getCreatedAt())
                .submittedAt(t.getSubmittedAt()).reviewedAt(t.getReviewedAt()).build();
    }

    private FeedbackAdminResponse feedback(Feedback f) {
        return FeedbackAdminResponse.builder().id(f.getId()).internId(f.getIntern().getId()).internName(f.getIntern().getUser().getName())
                .managerId(f.getManager().getId()).managerName(f.getManager().getName()).taskId(f.getTask() == null ? null : f.getTask().getId())
                .taskTitle(f.getTask() == null ? null : f.getTask().getTitle()).rating(f.getRating()).feedbackText(f.getFeedbackText()).createdAt(f.getCreatedAt()).build();
    }

    private InterviewAdminResponse interview(Interview i) {
        return InterviewAdminResponse.builder().id(i.getId()).candidateId(i.getCandidate() == null ? null : i.getCandidate().getId())
                .candidateName(i.getCandidate() == null ? null : i.getCandidate().getName()).internId(i.getIntern() == null ? null : i.getIntern().getId())
                .internName(i.getIntern() == null ? null : i.getIntern().getUser().getName()).role(i.getRole()).status(i.getStatus().name())
                .scheduledAt(i.getScheduledAt()).completedAt(i.getCompletedAt()).finalScore(i.getFinalScore()).recommendation(i.getRecommendation()).build();
    }

    private InterviewResultAdminResponse interviewResult(InterviewResult r) {
        Interview i = r.getInterview();
        String participant = i.getIntern() != null ? i.getIntern().getUser().getName() : i.getCandidate() == null ? "Candidate" : i.getCandidate().getName();
        return InterviewResultAdminResponse.builder().id(r.getId()).interviewId(i.getId()).participantName(participant)
                .technicalScore(r.getTechnicalScore()).communicationScore(r.getCommunicationScore()).problemSolvingScore(r.getProblemSolvingScore())
                .confidenceScore(r.getConfidenceScore()).finalScore(r.getFinalScore()).recommendation(r.getRecommendation()).aiSummary(r.getAiSummary()).build();
    }

    private SettingResponse setting(SystemSetting s) {
        return SettingResponse.builder().key(s.getKey()).value(s.getValue()).description(s.getDescription()).build();
    }

    private SummaryCards summaryCards(List<InternProfile> interns, List<Task> tasks, List<Attendance> attendance, List<InternScoreResponse> scores) {
        List<User> users = userRepository.findAll();
        return SummaryCards.builder()
                .totalUsers(users.size()).totalInterns(users.stream().filter(u -> u.getRole() == Role.INTERN).count())
                .totalManagers(users.stream().filter(u -> u.getRole() == Role.MANAGER).count()).totalHR(users.stream().filter(u -> u.getRole() == Role.HR).count())
                .totalCandidates(candidateRepository.count()).activeInterns(interns.stream().filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus())).count())
                .completedInternships(interns.stream().filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus())).count()).totalDepartments(departmentRepository.count())
                .totalAssignedCompanies(assignedCompanyRepository.count()).totalTasks(tasks.size()).pendingReviews(tasks.stream().filter(t -> t.getStatus() == TaskStatus.SUBMITTED).count())
                .averageAttendance(attendancePercentage(attendance)).averageInternScore(average(scores.stream().map(InternScoreResponse::getFinalScore).toList())).build();
    }

    private InternScoreResponse internScore(InternProfile intern, List<Attendance> attendance, List<Task> tasks, List<Feedback> feedback, String reason) {
        BigDecimal attendancePct = attendancePercentage(attendance);
        BigDecimal taskPct = taskCompletion(tasks);
        BigDecimal feedbackPct = averageRating(feedback).multiply(BigDecimal.valueOf(20));
        BigDecimal finalScore = attendancePct.multiply(BigDecimal.valueOf(0.35)).add(taskPct.multiply(BigDecimal.valueOf(0.40))).add(feedbackPct.multiply(BigDecimal.valueOf(0.25))).setScale(1, RoundingMode.HALF_UP);
        return InternScoreResponse.builder().internId(intern.getId()).empId(firstNonBlank(intern.getEmpId(), intern.getUser().getEmpId()))
                .name(intern.getUser().getName()).department(departmentName(intern)).assignedCompany(firstNonBlank(intern.getAssignedCompany(), "Unassigned"))
                .reason(firstNonBlank(reason, finalScore.compareTo(BigDecimal.valueOf(60)) < 0 ? "Low overall score" : "Healthy performance"))
                .attendancePercentage(attendancePct).taskCompletionPercentage(taskPct).finalScore(finalScore).build();
    }

    private List<ManagerPerformanceResponse> managerPerformanceRows(List<InternProfile> interns, List<Task> tasks, List<Feedback> feedback) {
        return userRepository.findByRoleOrderByNameAsc(Role.MANAGER).stream().map(manager -> {
            List<InternProfile> assigned = interns.stream().filter(i -> i.getManager() != null && Objects.equals(i.getManager().getId(), manager.getId())).toList();
            List<InternScoreResponse> scores = assigned.stream().map(i -> internScore(i, attendanceRepository.findByIntern_IdOrderByDateDesc(i.getId()), tasksFor(tasks, i), feedbackFor(feedback, i), null)).toList();
            List<Feedback> managerFeedback = feedback.stream().filter(f -> Objects.equals(f.getManager().getId(), manager.getId())).toList();
            return ManagerPerformanceResponse.builder().managerId(manager.getId()).name(manager.getName()).department(manager.getDepartment())
                    .assignedInterns(assigned.size()).averageInternScore(average(scores.stream().map(InternScoreResponse::getFinalScore).toList()))
                    .pendingReviews(tasks.stream().filter(t -> Objects.equals(t.getAssignedBy().getId(), manager.getId()) && t.getStatus() == TaskStatus.SUBMITTED).count())
                    .averageFeedbackRating(averageRating(managerFeedback)).build();
        }).toList();
    }

    private List<ActivityResponse> recentActivity() {
        return auditLogRepository.findAllByOrderByTimestampDesc(org.springframework.data.domain.PageRequest.of(0, 8)).stream()
                .map(log -> ActivityResponse.builder().action(log.getActionType()).actorName(log.getActor() == null ? "System" : log.getActor().getName())
                        .entityType(log.getEntityName()).description(log.getActionType() + " on " + log.getEntityName()).createdAt(log.getTimestamp()).build())
                .toList();
    }

    private Map<String, Object> taskSummary(List<Task> tasks) {
        return Map.of(
                "totalTasks", tasks.size(),
                "pending", tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count(),
                "inProgress", tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count(),
                "submitted", tasks.stream().filter(t -> t.getStatus() == TaskStatus.SUBMITTED).count(),
                "completed", tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.APPROVED).count(),
                "rejected", tasks.stream().filter(t -> t.getStatus() == TaskStatus.REJECTED).count(),
                "averageRating", averageTaskRating(tasks)
        );
    }

    private Map<String, Object> attendanceSummary(List<Attendance> attendance) {
        return Map.of(
                "totalRecords", attendance.size(),
                "present", attendance.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count(),
                "absent", attendance.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count(),
                "halfDay", attendance.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count(),
                "averageAttendance", attendancePercentage(attendance),
                "totalWorkingHours", attendance.stream().map(Attendance::getTotalHours).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private Map<String, Long> countBy(Stream<String> values) {
        return values.map(v -> firstNonBlank(v, "Unassigned")).collect(Collectors.groupingBy(v -> v, LinkedHashMap::new, Collectors.counting()));
    }

    private Map<String, BigDecimal> groupAverage(List<InternScoreResponse> scores, java.util.function.Function<InternScoreResponse, String> keyFn) {
        return scores.stream().collect(Collectors.groupingBy(score -> firstNonBlank(keyFn.apply(score), "Unassigned"), LinkedHashMap::new,
                Collectors.collectingAndThen(Collectors.mapping(InternScoreResponse::getFinalScore, Collectors.toList()), this::average)));
    }

    private List<Task> tasksFor(List<Task> tasks, InternProfile intern) {
        return tasks.stream().filter(t -> Objects.equals(t.getAssignedTo().getId(), intern.getId())).toList();
    }

    private List<Attendance> attendanceFor(List<Attendance> attendance, InternProfile intern) {
        return attendance.stream().filter(a -> Objects.equals(a.getIntern().getId(), intern.getId())).toList();
    }

    private List<Feedback> feedbackFor(List<Feedback> feedback, InternProfile intern) {
        return feedback.stream().filter(f -> Objects.equals(f.getIntern().getId(), intern.getId())).toList();
    }

    private BigDecimal attendancePercentage(List<Attendance> records) {
        if (records.isEmpty()) return BigDecimal.ZERO;
        long present = records.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long half = records.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();
        return BigDecimal.valueOf((present + (half * 0.5)) * 100 / records.size()).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal taskCompletion(List<Task> tasks) {
        if (tasks.isEmpty()) return BigDecimal.ZERO;
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.APPROVED).count();
        return BigDecimal.valueOf(completed * 100.0 / tasks.size()).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal averageRating(List<Feedback> feedback) {
        return BigDecimal.valueOf(feedback.stream().filter(f -> f.getRating() != null).mapToInt(Feedback::getRating).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal averageTaskRating(List<Task> tasks) {
        return BigDecimal.valueOf(tasks.stream().filter(t -> t.getRating() != null).mapToInt(Task::getRating).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) return BigDecimal.ZERO;
        return nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(nonNull.size()), 1, RoundingMode.HALF_UP);
    }

    private boolean contains(String search, String... values) {
        if (search == null || search.isBlank()) return true;
        String query = search.toLowerCase(Locale.ROOT);
        return Arrays.stream(values).filter(Objects::nonNull).anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(query));
    }

    private boolean isActive(String status) {
        return !"INACTIVE".equalsIgnoreCase(firstNonBlank(status, "ACTIVE")) && !"BLOCKED".equalsIgnoreCase(firstNonBlank(status, "ACTIVE"));
    }

    private String status(String status) {
        String normalized = firstNonBlank(status, "ACTIVE").toUpperCase(Locale.ROOT);
        return Set.of("ACTIVE", "INACTIVE", "BLOCKED", "COMPLETED").contains(normalized) ? normalized : "ACTIVE";
    }

    private String designation(Role role) {
        return switch (role) {
            case INTERN -> "Intern";
            case MANAGER -> "Manager";
            case HR -> "HR";
            case ADMIN -> "Admin";
        };
    }

    private String departmentName(InternProfile intern) {
        return intern.getDepartment() == null ? "Unassigned" : intern.getDepartment().getName();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder("IQ-");
        for (int i = 0; i < 14; i++) {
            password.append(PASSWORD_CHARS[SECURE_RANDOM.nextInt(PASSWORD_CHARS.length)]);
        }
        return password.toString();
    }

    private void seedSettingsIfMissing() {
        if (systemSettingRepository.count() > 0) return;
        systemSettingRepository.save(SystemSetting.builder().key("sessionTimeoutMinutes").value("30").description("Frontend auto logout timeout.").build());
        systemSettingRepository.save(SystemSetting.builder().key("mockAiEnabled").value("true").description("AI provider defaults to MOCK unless configured otherwise.").build());
        systemSettingRepository.save(SystemSetting.builder().key("emailProvider").value("mock").description("Email notifications are stored/mocked by default.").build());
    }
}
