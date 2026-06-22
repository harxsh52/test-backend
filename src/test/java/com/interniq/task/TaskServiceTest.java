package com.interniq.task;

import com.interniq.audit.AuditLogService;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileService;
import com.interniq.notification.EmailService;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.task.dto.TaskRequest;
import com.interniq.task.dto.TaskReviewRequest;
import com.interniq.task.dto.TaskStatusRequest;
import com.interniq.task.dto.TaskSubmitRequest;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private InternProfileService internProfileService;

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void managerAssignsTaskSuccessfully() {
        User manager = user(2L, Role.MANAGER);
        InternProfile profile = profile(10L, user(1L, Role.INTERN), manager);
        Authentication authentication = authentication(manager);
        TaskRequest request = taskRequest();

        when(userService.getCurrentUser(authentication)).thenReturn(manager);
        when(internProfileService.getProfileOrThrow(10L)).thenReturn(profile);
        when(internProfileService.isManagerOf(manager, profile)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(50L);
            task.setCreatedAt(LocalDateTime.now());
            return task;
        });

        var response = taskService.createTask(request, authentication);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.getAssignedToInternId()).isEqualTo(10L);
        verify(auditLogService).record(manager, "TASK_ASSIGNED", "Task", 50L);
        verify(notificationService).createNotification(
                profile.getUser(),
                "New Task Assigned",
                "You have been assigned a new task: Build login",
                NotificationType.TASK,
                NotificationPriority.HIGH,
                "/intern/tasks/50",
                null
        );
        verify(emailService).sendTaskAssigned(any(Task.class));
    }

    @Test
    void internCanViewOwnTasks() {
        User intern = user(1L, Role.INTERN);
        User manager = user(2L, Role.MANAGER);
        InternProfile profile = profile(10L, intern, manager);
        Task task = task(50L, profile, manager, TaskStatus.PENDING);
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(taskRepository.findByAssignedTo_User_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(task));

        var tasks = taskService.getMyTasks(authentication);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("Build login");
    }

    @Test
    void internUpdatesTaskStatus() {
        User intern = user(1L, Role.INTERN);
        User manager = user(2L, Role.MANAGER);
        InternProfile profile = profile(10L, intern, manager);
        Task task = task(50L, profile, manager, TaskStatus.PENDING);
        TaskStatusRequest request = new TaskStatusRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));

        var response = taskService.updateTaskStatus(50L, request, authentication);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void internSubmitsTaskWithLinkAndNote() {
        User intern = user(1L, Role.INTERN);
        User manager = user(2L, Role.MANAGER);
        InternProfile profile = profile(10L, intern, manager);
        Task task = task(50L, profile, manager, TaskStatus.IN_PROGRESS);
        TaskSubmitRequest request = new TaskSubmitRequest();
        request.setSubmissionLink("https://github.com/example/project");
        request.setSubmissionNote("Completed the API integration.");
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));

        var response = taskService.submitTask(50L, request, authentication);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
        assertThat(response.getSubmissionLink()).isEqualTo("https://github.com/example/project");
        assertThat(response.getSubmissionNote()).isEqualTo("Completed the API integration.");
        assertThat(response.getSubmittedAt()).isNotNull();
        verify(auditLogService).record(intern, "TASK_SUBMITTED", "Task", 50L);
    }

    @Test
    void managerApprovesTask() {
        User manager = user(2L, Role.MANAGER);
        InternProfile profile = profile(10L, user(1L, Role.INTERN), manager);
        Task task = task(50L, profile, manager, TaskStatus.SUBMITTED);
        TaskReviewRequest request = reviewRequest(TaskStatus.APPROVED);
        Authentication authentication = authentication(manager);

        when(userService.getCurrentUser(authentication)).thenReturn(manager);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));

        var response = taskService.reviewTask(50L, request, authentication);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.APPROVED);
        assertThat(response.getManagerFeedback()).isEqualTo("Good work.");
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getReviewedAt()).isNotNull();
        verify(auditLogService).record(manager, "TASK_REVIEWED", "Task", 50L);
        verify(emailService).sendTaskReviewed(task);
    }

    @Test
    void managerRejectsTask() {
        User manager = user(2L, Role.MANAGER);
        InternProfile profile = profile(10L, user(1L, Role.INTERN), manager);
        Task task = task(50L, profile, manager, TaskStatus.SUBMITTED);
        TaskReviewRequest request = reviewRequest(TaskStatus.REJECTED);
        request.setManagerFeedback("Please add tests.");
        Authentication authentication = authentication(manager);

        when(userService.getCurrentUser(authentication)).thenReturn(manager);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));

        var response = taskService.reviewTask(50L, request, authentication);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(response.getManagerFeedback()).isEqualTo("Please add tests.");
    }

    @Test
    void unauthorizedRoleCannotReviewTask() {
        User intern = user(1L, Role.INTERN);
        Authentication authentication = authentication(intern);
        TaskReviewRequest request = reviewRequest(TaskStatus.APPROVED);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);

        assertThatThrownBy(() -> taskService.reviewTask(50L, request, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only managers");
    }

    @Test
    void internCannotApproveTaskThroughStatusEndpoint() {
        User intern = user(1L, Role.INTERN);
        User manager = user(2L, Role.MANAGER);
        InternProfile profile = profile(10L, intern, manager);
        Task task = task(50L, profile, manager, TaskStatus.PENDING);
        TaskStatusRequest request = new TaskStatusRequest();
        request.setStatus(TaskStatus.APPROVED);
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTaskStatus(50L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Approval and rejection");
    }

    private TaskRequest taskRequest() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Build login");
        request.setDescription("Connect the API");
        request.setAssignedToInternId(10L);
        request.setPriority(Priority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(3));
        return request;
    }

    private TaskReviewRequest reviewRequest(TaskStatus status) {
        TaskReviewRequest request = new TaskReviewRequest();
        request.setStatus(status);
        request.setManagerFeedback("Good work.");
        request.setRating(4);
        return request;
    }

    private Task task(Long id, InternProfile profile, User manager, TaskStatus status) {
        return Task.builder()
                .id(id)
                .title("Build login")
                .description("Connect the API")
                .assignedTo(profile)
                .assignedBy(manager)
                .priority(Priority.HIGH)
                .status(status)
                .dueDate(LocalDate.now().plusDays(3))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private InternProfile profile(Long id, User intern, User manager) {
        return InternProfile.builder()
                .id(id)
                .user(intern)
                .manager(manager)
                .status("ACTIVE")
                .build();
    }

    private Authentication authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .name(role.name())
                .email(role.name().toLowerCase() + "@test.com")
                .password("encoded")
                .role(role)
                .active(true)
                .build();
    }
}
