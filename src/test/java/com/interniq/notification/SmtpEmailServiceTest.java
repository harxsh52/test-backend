package com.interniq.notification;

import com.interniq.intern.InternProfile;
import com.interniq.task.Priority;
import com.interniq.task.Task;
import com.interniq.user.Role;
import com.interniq.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailServiceTest {

    private JavaMailSender mailSender;
    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new SmtpEmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "appName", "InternIQ");
        ReflectionTestUtils.setField(emailService, "appUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(emailService, "failOnError", false);
    }

    @Test
    void sendTaskAssignedSendsEmailToIntern() {
        Task task = Task.builder()
                .title("Build login page")
                .description("Create the React login screen")
                .priority(Priority.HIGH)
                .assignedTo(profile(user(1L, Role.INTERN, "intern@test.com")))
                .assignedBy(user(2L, Role.MANAGER, "manager@test.com"))
                .build();

        emailService.sendTaskAssigned(task);

        var messageCaptor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@test.com");
        assertThat(message.getTo()).containsExactly("intern@test.com");
        assertThat(message.getSubject()).contains("New task assigned");
        assertThat(message.getText()).contains("Build login page", "HIGH", "http://localhost:5173");
    }

    private InternProfile profile(User user) {
        return InternProfile.builder()
                .id(10L)
                .user(user)
                .status("ACTIVE")
                .build();
    }

    private User user(Long id, Role role, String email) {
        return User.builder()
                .id(id)
                .name(role.name())
                .email(email)
                .role(role)
                .active(true)
                .build();
    }
}
