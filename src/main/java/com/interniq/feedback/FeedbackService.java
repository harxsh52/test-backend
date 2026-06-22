package com.interniq.feedback;

import com.interniq.feedback.dto.FeedbackRequest;
import com.interniq.feedback.dto.FeedbackResponse;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.intern.InternProfileService;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.task.Task;
import com.interniq.task.TaskService;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final InternProfileRepository internProfileRepository;
    private final InternProfileService internProfileService;
    private final TaskService taskService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        if (currentUser.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only managers can create feedback");
        }

        InternProfile intern = internProfileService.getProfileOrThrow(request.getInternId());

        if (!internProfileService.isManagerOf(currentUser, intern)) {
            throw new AccessDeniedException("Managers can give feedback only to their assigned interns");
        }

        Task task = getTaskIfPresent(request.getTaskId(), intern, currentUser);

        Feedback feedback = Feedback.builder()
                .intern(intern)
                .manager(currentUser)
                .task(task)
                .feedbackText(clean(request.getFeedbackText()))
                .rating(request.getRating())
                .build();

        Feedback savedFeedback = feedbackRepository.save(feedback);
        notificationService.createNotification(
                intern.getUser(),
                "New Feedback Received",
                "You received feedback from " + currentUser.getName(),
                NotificationType.FEEDBACK,
                NotificationPriority.MEDIUM,
                "/intern/feedback",
                null
        );

        return toResponse(savedFeedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMyFeedback(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        if (currentUser.getRole() != Role.INTERN) {
            throw new AccessDeniedException("Only interns can view personal feedback here");
        }

        return feedbackRepository.findByIntern_User_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackForIntern(Long internId, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        InternProfile intern = internProfileService.getProfileOrThrow(internId);
        internProfileService.ensureCanViewProfile(currentUser, intern);

        return feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(internId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Task getTaskIfPresent(Long taskId, InternProfile intern, User manager) {
        if (taskId == null) {
            return null;
        }

        Task task = taskService.getTaskOrThrow(taskId);

        if (!Objects.equals(task.getAssignedTo().getId(), intern.getId())) {
            throw new IllegalArgumentException("Task does not belong to the selected intern");
        }

        if (!Objects.equals(task.getAssignedBy().getId(), manager.getId())) {
            throw new AccessDeniedException("Managers can add feedback only for tasks they assigned");
        }

        return task;
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        Task task = feedback.getTask();

        return FeedbackResponse.builder()
                .id(feedback.getId())
                .internId(feedback.getIntern().getId())
                .internName(feedback.getIntern().getUser().getName())
                .managerId(feedback.getManager().getId())
                .managerName(feedback.getManager().getName())
                .taskId(task == null ? null : task.getId())
                .taskTitle(task == null ? null : task.getTitle())
                .feedbackText(feedback.getFeedbackText())
                .rating(feedback.getRating())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
