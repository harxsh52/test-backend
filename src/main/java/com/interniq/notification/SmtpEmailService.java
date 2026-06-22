package com.interniq.notification;

import com.interniq.candidate.Candidate;
import com.interniq.interview.Interview;
import com.interniq.task.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "application.email", name = "provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JavaMailSender mailSender;

    @Value("${application.email.from:no-reply@interniq.local}")
    private String fromEmail;

    @Value("${application.email.app-name:InternIQ}")
    private String appName;

    @Value("${application.email.app-url:http://localhost:5173}")
    private String appUrl;

    @Value("${application.email.fail-on-error:false}")
    private boolean failOnError;

    @Override
    public void sendTaskAssigned(Task task) {
        String recipient = task.getAssignedTo().getUser().getEmail();
        String managerName = task.getAssignedBy() == null ? "your manager" : task.getAssignedBy().getName();

        send(
                recipient,
                appName + ": New task assigned - " + task.getTitle(),
                """
                        Hello %s,

                        A new task has been assigned to you in %s.

                        Task: %s
                        Priority: %s
                        Due date: %s
                        Assigned by: %s

                        Description:
                        %s

                        Open the app:
                        %s

                        Regards,
                        %s
                        """.formatted(
                        task.getAssignedTo().getUser().getName(),
                        appName,
                        task.getTitle(),
                        task.getPriority(),
                        task.getDueDate() == null ? "Not set" : task.getDueDate(),
                        managerName,
                        blankFallback(task.getDescription(), "No description provided."),
                        appUrl,
                        appName
                )
        );
    }

    @Override
    public void sendTaskReviewed(Task task) {
        String recipient = task.getAssignedTo().getUser().getEmail();

        send(
                recipient,
                appName + ": Task reviewed - " + task.getTitle(),
                """
                        Hello %s,

                        Your task has been reviewed in %s.

                        Task: %s
                        Status: %s
                        Rating: %s

                        Manager feedback:
                        %s

                        Open the app:
                        %s

                        Regards,
                        %s
                        """.formatted(
                        task.getAssignedTo().getUser().getName(),
                        appName,
                        task.getTitle(),
                        task.getStatus(),
                        task.getRating() == null ? "Not rated" : task.getRating() + "/5",
                        blankFallback(task.getManagerFeedback(), "No feedback note provided."),
                        appUrl,
                        appName
                )
        );
    }

    @Override
    public void sendInterviewScheduled(Interview interview) {
        String recipient = interview.getIntern() != null
                ? interview.getIntern().getUser().getEmail()
                : interview.getCandidate().getEmail();
        String recipientName = interview.getIntern() != null
                ? interview.getIntern().getUser().getName()
                : interview.getCandidate().getName();

        send(
                recipient,
                appName + ": Interview scheduled for " + interview.getRole(),
                """
                        Hello %s,

                        Your interview has been scheduled in %s.

                        Role: %s
                        Scheduled at: %s
                        Status: %s

                        Open the app:
                        %s

                        Regards,
                        %s
                        """.formatted(
                        recipientName,
                        appName,
                        interview.getRole(),
                        interview.getScheduledAt() == null ? "To be confirmed" : interview.getScheduledAt().format(DATE_TIME_FORMATTER),
                        interview.getStatus(),
                        appUrl,
                        appName
                )
        );
    }

    @Override
    public void sendResumeScreened(Candidate candidate) {
        send(
                candidate.getEmail(),
                appName + ": Resume screening update",
                """
                        Hello %s,

                        Your resume screening has been completed in %s.

                        Applied role: %s
                        Current status: %s
                        Recommendation: %s

                        Regards,
                        %s
                        """.formatted(
                        candidate.getName(),
                        appName,
                        candidate.getAppliedRole(),
                        candidate.getStatus(),
                        blankFallback(candidate.getAiRecommendation(), "Under review"),
                        appName
                )
        );
    }

    @Override
    public void sendTestEmail(String recipient) {
        send(
                recipient,
                appName + ": Test email",
                """
                        Hello,

                        This is a test email from %s.

                        If you received this message, SMTP email notifications are configured correctly.

                        App URL:
                        %s

                        Regards,
                        %s
                """.formatted(appName, appUrl, appName)
        );
    }

    @Override
    public void sendLetter(String recipient, String subject, String body) {
        send(recipient, subject, body);
    }

    private void send(String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Email skipped: recipient is missing for subject '{}'", subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", recipient, subject);
        } catch (MailException ex) {
            log.error("Email sending failed for recipient {} and subject '{}': {}", recipient, subject, ex.getMessage());
            if (failOnError) {
                throw ex;
            }
        }
    }

    private String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
