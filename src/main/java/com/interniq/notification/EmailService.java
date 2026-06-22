package com.interniq.notification;

import com.interniq.candidate.Candidate;
import com.interniq.interview.Interview;
import com.interniq.task.Task;

public interface EmailService {

    void sendTaskAssigned(Task task);

    void sendTaskReviewed(Task task);

    void sendInterviewScheduled(Interview interview);

    void sendResumeScreened(Candidate candidate);

    void sendTestEmail(String recipient);

    void sendLetter(String recipient, String subject, String body);
}
