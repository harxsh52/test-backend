package com.interniq.task;

import com.interniq.audit.AuditLogService;
import com.interniq.common.PageRequestFactory;
import com.interniq.common.PageResponse;
import com.interniq.common.PagingUtils;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileService;
import com.interniq.notification.EmailService;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.task.dto.TaskRequest;
import com.interniq.task.dto.TaskResponse;
import com.interniq.task.dto.TaskReviewRequest;
import com.interniq.task.dto.TaskStatusRequest;
import com.interniq.task.dto.TaskSubmitRequest;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final InternProfileService internProfileService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse createTask(TaskRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.MANAGER, "Only managers can assign tasks");

        InternProfile assignedIntern = internProfileService.getProfileOrThrow(request.getAssignedToInternId());

        if (!internProfileService.isManagerOf(currentUser, assignedIntern)) {
            throw new AccessDeniedException("Managers can assign tasks only to their assigned interns");
        }

        Task task = Task.builder()
                .title(clean(request.getTitle()))
                .description(clean(request.getDescription()))
                .assignedTo(assignedIntern)
                .assignedBy(currentUser)
                .priority(request.getPriority() == null ? Priority.MEDIUM : request.getPriority())
                .status(TaskStatus.PENDING)
                .dueDate(request.getDueDate())
                .build();

        Task savedTask = taskRepository.save(task);
        auditLogService.record(currentUser, "TASK_ASSIGNED", "Task", savedTask.getId());
        notificationService.createNotification(
                assignedIntern.getUser(),
                "New Task Assigned",
                "You have been assigned a new task: " + savedTask.getTitle(),
                NotificationType.TASK,
                NotificationPriority.HIGH,
                "/intern/tasks/" + savedTask.getId(),
                null
        );
        emailService.sendTaskAssigned(savedTask);
        return toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(Authentication authentication) {
        return getMyTasks(null, null, null, null, null, null, authentication);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(
            TaskStatus status,
            Priority priority,
            Long internId,
            Long managerId,
            LocalDate fromDate,
            LocalDate toDate,
            Authentication authentication
    ) {
        User currentUser = userService.getCurrentUser(authentication);

        List<Task> tasks = switch (currentUser.getRole()) {
            case INTERN -> taskRepository.findByAssignedTo_User_IdOrderByCreatedAtDesc(currentUser.getId());
            case MANAGER -> taskRepository.findByAssignedBy_IdOrderByCreatedAtDesc(currentUser.getId());
            case HR, ADMIN -> taskRepository.findAllByOrderByCreatedAtDesc();
        };

        return filterTasks(tasks, status, priority, internId, managerId, fromDate, toDate)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> searchMyTasks(
            TaskStatus status,
            Priority priority,
            Long internId,
            Long managerId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        Pageable pageable = taskPageable(page, size, sortBy, sortDirection);
        return PageResponse.from(
                PagingUtils.paginate(getMyTasks(status, priority, internId, managerId, fromDate, toDate, authentication), pageable),
                sortBy,
                sortDirection
        );
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksAssignedByMe(Authentication authentication) {
        return getTasksAssignedByMe(null, null, null, null, null, authentication);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksAssignedByMe(
            TaskStatus status,
            Priority priority,
            Long internId,
            LocalDate fromDate,
            LocalDate toDate,
            Authentication authentication
    ) {
        User currentUser = userService.getCurrentUser(authentication);

        List<Task> tasks = switch (currentUser.getRole()) {
            case MANAGER -> taskRepository.findByAssignedBy_IdOrderByCreatedAtDesc(currentUser.getId());
            case HR, ADMIN -> taskRepository.findAllByOrderByCreatedAtDesc();
            case INTERN -> throw new AccessDeniedException("Interns cannot view assigned-by-me tasks");
        };

        return filterTasks(tasks, status, priority, internId, currentUser.getRole() == Role.MANAGER ? currentUser.getId() : null, fromDate, toDate)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> searchTasksAssignedByMe(
            TaskStatus status,
            Priority priority,
            Long internId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        Pageable pageable = taskPageable(page, size, sortBy, sortDirection);
        return PageResponse.from(
                PagingUtils.paginate(getTasksAssignedByMe(status, priority, internId, fromDate, toDate, authentication), pageable),
                sortBy,
                sortDirection
        );
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Task task = getTaskOrThrow(id);
        ensureCanViewTask(currentUser, task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long id, TaskStatusRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Task task = getTaskOrThrow(id);
        ensureInternOwnsTask(currentUser, task);

        if (request.getStatus() == TaskStatus.APPROVED || request.getStatus() == TaskStatus.REJECTED) {
            throw new IllegalArgumentException("Approval and rejection are handled by manager review");
        }

        if (request.getStatus() == TaskStatus.SUBMITTED) {
            throw new IllegalArgumentException("Use the submit endpoint to submit work");
        }

        task.setStatus(request.getStatus());
        return toResponse(task);
    }

    @Transactional
    public TaskResponse submitTask(Long id, TaskSubmitRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Task task = getTaskOrThrow(id);
        ensureInternOwnsTask(currentUser, task);

        task.setSubmissionLink(clean(request.getSubmissionLink()));
        task.setSubmissionNote(clean(request.getSubmissionNote()));
        task.setStatus(TaskStatus.SUBMITTED);
        task.setSubmittedAt(LocalDateTime.now());
        auditLogService.record(currentUser, "TASK_SUBMITTED", "Task", task.getId());
        notificationService.createNotification(
                task.getAssignedBy(),
                "Task Submitted",
                currentUser.getName() + " submitted task: " + task.getTitle(),
                NotificationType.TASK,
                NotificationPriority.HIGH,
                "/manager/review-tasks/" + task.getId(),
                null
        );

        return toResponse(task);
    }

    @Transactional
    public TaskResponse reviewTask(Long id, TaskReviewRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.MANAGER, "Only managers can review tasks");

        Task task = getTaskOrThrow(id);

        if (!Objects.equals(task.getAssignedBy().getId(), currentUser.getId())) {
            throw new AccessDeniedException("Managers can review only tasks they assigned");
        }

        if (request.getStatus() != TaskStatus.APPROVED
                && request.getStatus() != TaskStatus.REJECTED
                && request.getStatus() != TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Review status must be APPROVED, REJECTED, or COMPLETED");
        }

        task.setStatus(request.getStatus());
        task.setManagerFeedback(clean(request.getManagerFeedback()));
        task.setRating(request.getRating());
        task.setReviewedAt(LocalDateTime.now());
        auditLogService.record(currentUser, "TASK_REVIEWED", "Task", task.getId());
        notificationService.createNotification(
                task.getAssignedTo().getUser(),
                "Task Reviewed",
                "Your task " + task.getTitle() + " was " + task.getStatus(),
                NotificationType.TASK,
                NotificationPriority.MEDIUM,
                "/intern/tasks/" + task.getId(),
                null
        );
        emailService.sendTaskReviewed(task);

        return toResponse(task);
    }

    public Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .assignedToInternId(task.getAssignedTo().getId())
                .assignedToName(task.getAssignedTo().getUser().getName())
                .assignedByUserId(task.getAssignedBy().getId())
                .assignedByName(task.getAssignedBy().getName())
                .priority(task.getPriority())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .submissionLink(task.getSubmissionLink())
                .submissionNote(task.getSubmissionNote())
                .managerFeedback(task.getManagerFeedback())
                .rating(task.getRating())
                .createdAt(task.getCreatedAt())
                .submittedAt(task.getSubmittedAt())
                .reviewedAt(task.getReviewedAt())
                .build();
    }

    private void ensureCanViewTask(User currentUser, Task task) {
        if (isHrOrAdmin(currentUser)) {
            return;
        }

        if (currentUser.getRole() == Role.INTERN
                && Objects.equals(task.getAssignedTo().getUser().getId(), currentUser.getId())) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER
                && (Objects.equals(task.getAssignedBy().getId(), currentUser.getId())
                || internProfileService.isManagerOf(currentUser, task.getAssignedTo()))) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to access this task");
    }

    private void ensureInternOwnsTask(User currentUser, Task task) {
        ensureRole(currentUser, Role.INTERN, "Only interns can update or submit their tasks");

        if (!Objects.equals(task.getAssignedTo().getUser().getId(), currentUser.getId())) {
            throw new AccessDeniedException("Interns can update only their own tasks");
        }
    }

    private void ensureRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new AccessDeniedException(message);
        }
    }

    private boolean isHrOrAdmin(User user) {
        return user.getRole() == Role.HR || user.getRole() == Role.ADMIN;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private java.util.stream.Stream<Task> filterTasks(
            List<Task> tasks,
            TaskStatus status,
            Priority priority,
            Long internId,
            Long managerId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return tasks.stream()
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> priority == null || task.getPriority() == priority)
                .filter(task -> internId == null || Objects.equals(task.getAssignedTo().getId(), internId))
                .filter(task -> managerId == null || Objects.equals(task.getAssignedBy().getId(), managerId))
                .filter(task -> fromDate == null || !task.getCreatedAt().toLocalDate().isBefore(fromDate))
                .filter(task -> toDate == null || !task.getCreatedAt().toLocalDate().isAfter(toDate));
    }

    private Pageable taskPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        return PageRequestFactory.create(
                page,
                size,
                sortBy,
                sortDirection,
                Set.of("id", "createdAt", "dueDate", "priority", "status", "rating"),
                "createdAt"
        );
    }
}
