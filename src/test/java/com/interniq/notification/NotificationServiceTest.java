package com.interniq.notification;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void userCanFetchOwnNotifications() {
        User intern = user(1L, Role.INTERN);
        Notification notification = notification(100L, intern, NotificationStatus.UNREAD);
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(notificationRepository.findByRecipientUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));

        var notifications = notificationService.getMyNotifications(authentication);

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getId()).isEqualTo(100L);
        assertThat(notifications.get(0).getRecipientUserId()).isEqualTo(1L);
        assertThat(notifications.get(0).getStatus()).isEqualTo(NotificationStatus.UNREAD);
        verify(notificationRepository).findByRecipientUser_IdOrderByCreatedAtDesc(1L);
    }

    @Test
    void unreadCountWorks() {
        User intern = user(1L, Role.INTERN);
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(notificationRepository.countByRecipientUser_IdAndStatus(1L, NotificationStatus.UNREAD)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount(authentication)).isEqualTo(3);
    }

    @Test
    void markAsReadWorks() {
        User intern = user(1L, Role.INTERN);
        Notification notification = notification(100L, intern, NotificationStatus.UNREAD);
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(notificationRepository.findByIdAndRecipientUser_Id(100L, 1L)).thenReturn(Optional.of(notification));

        var response = notificationService.markAsRead(100L, authentication);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(response.getReadAt()).isNotNull();
    }

    @Test
    void userCannotReadAnotherUsersNotification() {
        User intern = user(1L, Role.INTERN);
        Authentication authentication = authentication(intern);

        when(userService.getCurrentUser(authentication)).thenReturn(intern);
        when(notificationRepository.findByIdAndRecipientUser_Id(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(100L, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    private Notification notification(Long id, User recipient, NotificationStatus status) {
        return Notification.builder()
                .id(id)
                .recipientUser(recipient)
                .title("New Task Assigned")
                .message("You have been assigned a new task: Build login")
                .type(NotificationType.TASK)
                .priority(NotificationPriority.HIGH)
                .status(status)
                .actionUrl("/intern/tasks/50")
                .createdAt(LocalDateTime.now())
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
