package com.interniq.notification;

import com.interniq.candidate.Candidate;
import com.interniq.interview.Interview;
import com.interniq.task.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "application.email", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailService implements EmailService {

    @Override
    public void sendTaskAssigned(Task task) {
        log.info("Mock email: task '{}' assigned to {}", task.getTitle(), task.getAssignedTo().getUser().getEmail());
    }

    @Override
    public void sendTaskReviewed(Task task) {
        log.info("Mock email: task '{}' reviewed for {}", task.getTitle(), task.getAssignedTo().getUser().getEmail());
    }

    @Override
    public void sendInterviewScheduled(Interview interview) {
        String recipient = interview.getIntern() != null
                ? interview.getIntern().getUser().getEmail()
                : interview.getCandidate().getEmail();
        log.info("Mock email: interview {} scheduled for {}", interview.getId(), recipient);
    }

    @Override
    public void sendResumeScreened(Candidate candidate) {
        log.info("Mock email: resume screening completed for {}", candidate.getEmail());
    }

    @Override
    public void sendTestEmail(String recipient) {
        log.info("Mock email: test email sent to {}", recipient);
    }

    @Override
    public void sendLetter(String recipient, String subject, String body) {
        log.info("Mock email: letter '{}' sent to {}", subject, recipient);
    }
}
