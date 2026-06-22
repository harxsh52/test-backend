package com.interniq.report;

import com.interniq.attendance.Attendance;
import com.interniq.attendance.AttendanceRepository;
import com.interniq.candidate.CandidateRepository;
import com.interniq.department.Department;
import com.interniq.department.DepartmentRepository;
import com.interniq.feedback.Feedback;
import com.interniq.feedback.FeedbackRepository;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.task.Priority;
import com.interniq.task.Task;
import com.interniq.task.TaskRepository;
import com.interniq.task.TaskStatus;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void internCanViewOwnReport() {
        User internUser = user(1L, Role.INTERN);
        User manager = user(2L, Role.MANAGER);
        Department department = department();
        InternProfile intern = profile(10L, internUser, manager, department);
        Authentication authentication = authentication(internUser);

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(internProfileRepository.findByUserId(1L)).thenReturn(Optional.of(intern));
        mockInternReportData(intern, manager);

        var report = reportService.getInternReport(10L, authentication);

        assertThat(report.getInternId()).isEqualTo(10L);
        assertThat(report.getName()).isEqualTo("INTERN");
        assertThat(report.getDepartmentName()).isEqualTo("Engineering");
        assertThat(report.getTasksAssigned()).isEqualTo(1L);
        assertThat(report.getFinalExperienceScore()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void managerCanViewAssignedInternReport() {
        User manager = user(2L, Role.MANAGER);
        InternProfile intern = profile(10L, user(1L, Role.INTERN), manager, department());
        Authentication authentication = authentication(manager);

        when(userService.getCurrentUser(authentication)).thenReturn(manager);
        when(internProfileRepository.findByManager_Id(2L)).thenReturn(List.of(intern));
        mockInternReportData(intern, manager);

        var report = reportService.getInternReport(10L, authentication);

        assertThat(report.getInternId()).isEqualTo(10L);
        assertThat(report.getManagerName()).isEqualTo("MANAGER");
    }

    @Test
    void hrAndAdminCanViewAllInternReports() {
        User hr = user(3L, Role.HR);
        User admin = user(4L, Role.ADMIN);
        User manager = user(2L, Role.MANAGER);
        InternProfile intern = profile(10L, user(1L, Role.INTERN), manager, department());
        Authentication hrAuthentication = authentication(hr);
        Authentication adminAuthentication = authentication(admin);

        when(userService.getCurrentUser(hrAuthentication)).thenReturn(hr);
        when(userService.getCurrentUser(adminAuthentication)).thenReturn(admin);
        when(internProfileRepository.findAll()).thenReturn(List.of(intern));
        mockInternReportData(intern, manager);

        assertThat(reportService.getInternReport(10L, hrAuthentication).getInternId()).isEqualTo(10L);
        assertThat(reportService.getInternReport(10L, adminAuthentication).getInternId()).isEqualTo(10L);
    }

    @Test
    void unauthorizedUserCannotAccessRestrictedReport() {
        User manager = user(2L, Role.MANAGER);
        Authentication authentication = authentication(manager);

        when(userService.getCurrentUser(authentication)).thenReturn(manager);
        when(internProfileRepository.findByManager_Id(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> reportService.getInternReport(10L, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void adminDashboardReturnsSystemCounts() {
        User admin = user(4L, Role.ADMIN);
        Authentication authentication = authentication(admin);

        when(userService.getCurrentUser(authentication)).thenReturn(admin);
        when(userRepository.count()).thenReturn(10L);
        when(departmentRepository.count()).thenReturn(3L);
        when(userRepository.findByRoleOrderByNameAsc(Role.INTERN)).thenReturn(List.of(user(1L, Role.INTERN), user(5L, Role.INTERN)));
        when(userRepository.findByRoleOrderByNameAsc(Role.MANAGER)).thenReturn(List.of(user(2L, Role.MANAGER)));
        when(userRepository.findByRoleOrderByNameAsc(Role.HR)).thenReturn(List.of(user(3L, Role.HR)));
        when(userRepository.findAll()).thenReturn(List.of(admin, user(1L, Role.INTERN), user(2L, Role.MANAGER)));

        var response = reportService.getDashboardStats(authentication);

        assertThat(response.getTotalUsers()).isEqualTo(10L);
        assertThat(response.getTotalDepartments()).isEqualTo(3L);
        assertThat(response.getTotalInterns()).isEqualTo(2L);
        assertThat(response.getActiveUsers()).isEqualTo(3L);
    }

    @Test
    void hrAndAdminCanViewAllDepartmentReports() {
        User hr = user(3L, Role.HR);
        User admin = user(4L, Role.ADMIN);
        Department department = department();
        InternProfile intern = profile(10L, user(1L, Role.INTERN), user(2L, Role.MANAGER), department);
        Authentication hrAuthentication = authentication(hr);
        Authentication adminAuthentication = authentication(admin);

        when(userService.getCurrentUser(hrAuthentication)).thenReturn(hr);
        when(userService.getCurrentUser(adminAuthentication)).thenReturn(admin);
        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(department));
        when(internProfileRepository.findAll()).thenReturn(List.of(intern));
        when(attendanceRepository.findAll()).thenReturn(List.of(attendance(intern)));
        when(taskRepository.findAll()).thenReturn(List.of(task(intern, intern.getManager(), TaskStatus.APPROVED)));

        assertThat(reportService.getDepartmentReports(hrAuthentication)).hasSize(1);
        assertThat(reportService.getDepartmentReports(adminAuthentication)).hasSize(1);
    }

    private void mockInternReportData(InternProfile intern, User manager) {
        when(attendanceRepository.findByIntern_IdOrderByDateDesc(intern.getId())).thenReturn(List.of(attendance(intern)));
        when(taskRepository.findAll()).thenReturn(List.of(task(intern, manager, TaskStatus.APPROVED)));
        when(feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(intern.getId())).thenReturn(List.of(feedback(intern, manager)));
    }

    private Attendance attendance(InternProfile intern) {
        return Attendance.builder()
                .id(100L)
                .intern(intern)
                .date(LocalDate.now())
                .punchInTime(LocalTime.of(9, 0))
                .punchOutTime(LocalTime.of(17, 0))
                .totalHours(BigDecimal.valueOf(8))
                .status("PRESENT")
                .build();
    }

    private Task task(InternProfile intern, User manager, TaskStatus status) {
        return Task.builder()
                .id(50L)
                .title("Build login")
                .description("Connect the API")
                .assignedTo(intern)
                .assignedBy(manager)
                .priority(Priority.HIGH)
                .status(status)
                .dueDate(LocalDate.now().plusDays(2))
                .rating(4)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private Feedback feedback(InternProfile intern, User manager) {
        return Feedback.builder()
                .id(90L)
                .intern(intern)
                .manager(manager)
                .feedbackText("Strong ownership.")
                .rating(5)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private InternProfile profile(Long id, User user, User manager, Department department) {
        return InternProfile.builder()
                .id(id)
                .user(user)
                .manager(manager)
                .department(department)
                .skills("React, Spring Boot")
                .status("ACTIVE")
                .joiningDate(LocalDate.now().minusDays(30))
                .internshipStartDate(LocalDate.now().minusDays(30))
                .internshipEndDate(LocalDate.now().plusDays(30))
                .build();
    }

    private Department department() {
        return Department.builder()
                .id(20L)
                .name("Engineering")
                .description("Product engineering")
                .active(true)
                .build();
    }

    private Authentication authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .name(role.name())
                .email(role.name().toLowerCase() + id + "@test.com")
                .password("encoded")
                .role(role)
                .active(true)
                .build();
    }
}
