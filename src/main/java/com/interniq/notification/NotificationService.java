package com.interniq.notification;

import com.interniq.notification.dto.CreateNotificationRequest;
import com.interniq.notification.dto.NotificationResponse;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public Notification createNotification(
            User user,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            String actionUrl,
            String metadataJson
    ) {
        if (user == null) {
            return null;
        }

        return notificationRepository.save(Notification.builder()
                .recipientUser(user)
                .title(clean(title))
                .message(clean(message))
                .type(type == null ? NotificationType.SYSTEM : type)
                .priority(priority == null ? NotificationPriority.MEDIUM : priority)
                .status(NotificationStatus.UNREAD)
                .actionUrl(clean(actionUrl))
                .metadataJson(clean(metadataJson))
                .build());
    }

    @Transactional
    public List<NotificationResponse> notifyRole(
            Role role,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            String actionUrl
    ) {
        return notifyUsers(userRepository.findByRoleOrderByNameAsc(role), title, message, type, priority, actionUrl)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<Notification> notifyUsers(
            List<User> users,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            String actionUrl
    ) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        Map<Long, User> uniqueUsers = new LinkedHashMap<>();
        users.stream()
                .filter(user -> user != null && user.getId() != null)
                .forEach(user -> uniqueUsers.putIfAbsent(user.getId(), user));

        List<Notification> notifications = new ArrayList<>();
        uniqueUsers.values().forEach(user -> notifications.add(createNotification(user, title, message, type, priority, actionUrl, null)));
        return notifications;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        return notificationRepository.findByRecipientUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Authentication authentication) {
        return getUnreadCount(userService.getCurrentUser(authentication));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countByRecipientUser_IdAndStatus(currentUser.getId(), NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Authentication authentication) {
        return toResponse(markAsRead(notificationId, userService.getCurrentUser(authentication)));
    }

    @Transactional
    public Notification markAsRead(Long notificationId, User currentUser) {
        Notification notification = ownNotification(notificationId, currentUser);
        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
        }
        return notification;
    }

    @Transactional
    public List<NotificationResponse> markAllAsRead(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        LocalDateTime now = LocalDateTime.now();

        return notificationRepository.findByRecipientUser_IdAndStatusOrderByCreatedAtDesc(currentUser.getId(), NotificationStatus.UNREAD)
                .stream()
                .map(notification -> {
                    notification.setStatus(NotificationStatus.READ);
                    notification.setReadAt(now);
                    return toResponse(notification);
                })
                .toList();
    }

    @Transactional
    public NotificationResponse archive(Long notificationId, Authentication authentication) {
        return toResponse(archive(notificationId, userService.getCurrentUser(authentication)));
    }

    @Transactional
    public Notification archive(Long notificationId, User currentUser) {
        Notification notification = ownNotification(notificationId, currentUser);
        notification.setStatus(NotificationStatus.ARCHIVED);
        notification.setArchivedAt(LocalDateTime.now());
        return notification;
    }

    @Transactional
    public List<NotificationResponse> createSystemNotification(CreateNotificationRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can create system notifications");
        }

        Map<Long, User> recipients = new LinkedHashMap<>();

        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            userRepository.findAllById(request.getUserIds())
                    .forEach(user -> recipients.putIfAbsent(user.getId(), user));
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            request.getRoles().forEach(role ->
                    userRepository.findByRoleOrderByNameAsc(role).forEach(user -> recipients.putIfAbsent(user.getId(), user))
            );
        }

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Select at least one user or role");
        }

        return recipients.values()
                .stream()
                .map(user -> createNotification(
                        user,
                        request.getTitle(),
                        request.getMessage(),
                        request.getType() == null ? NotificationType.SYSTEM : request.getType(),
                        request.getPriority() == null ? NotificationPriority.MEDIUM : request.getPriority(),
                        request.getActionUrl(),
                        request.getMetadataJson()
                ))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can view all notifications");
        }

        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationResponse toResponse(Notification notification) {
        User recipient = notification.getRecipientUser();
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientUserId(recipient == null ? null : recipient.getId())
                .recipientName(recipient == null ? null : recipient.getName())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .priority(notification.getPriority())
                .status(notification.getStatus())
                .actionUrl(notification.getActionUrl())
                .metadataJson(notification.getMetadataJson())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .archivedAt(notification.getArchivedAt())
                .build();
    }

    private Notification ownNotification(Long notificationId, User currentUser) {
        return notificationRepository.findByIdAndRecipientUser_Id(notificationId, currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to access this notification"));
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
