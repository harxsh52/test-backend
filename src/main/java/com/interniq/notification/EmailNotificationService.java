package com.interniq.notification;

import com.interniq.audit.AuditLogService;
import com.interniq.candidate.Candidate;
import com.interniq.candidate.CandidateService;
import com.interniq.candidate.CandidateStatus;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileService;
import com.interniq.interview.Interview;
import com.interniq.interview.InterviewRepository;
import com.interniq.notification.dto.EmailNotificationResponse;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final EmailNotificationRepository emailNotificationRepository;
    private final CandidateService candidateService;
    private final InternProfileService internProfileService;
    private final InterviewRepository interviewRepository;
    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;
    private final UserService userService;

    @Value("${application.email.enabled:false}")
    private boolean mailEnabled;

    @Value("${application.email.from:no-reply@interniq.local}")
    private String fromEmail;

    @Value("${application.email.app-name:InternIQ}")
    private String appName;

    @Value("${application.email.fail-on-error:false}")
    private boolean failOnError;

    @Transactional
    public EmailNotificationResponse sendOfferLetter(Long candidateId, Authentication authentication) {
        Candidate candidate = candidateService.getCandidateOrThrow(candidateId);
        EmailTemplate template = template(
                "Offer Letter Sent - Internship Opportunity",
                """
                        Hello %s,

                        Congratulations. Your offer letter for the internship opportunity has been sent.
                        Please check the details and complete the required steps.

                        Regards,
                        HR Team
                        """.formatted(candidate.getName())
        );
        EmailNotification notification = send(candidate.getEmail(), candidate.getName(), template, EmailNotificationType.OFFER_LETTER_SENT, "Candidate", candidate.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional
    public EmailNotificationResponse sendInterviewEmail(Long interviewId, Authentication authentication) {
        Interview interview = getInterview(interviewId);
        EmailNotification notification = sendInterviewScheduled(interview, authentication);
        return toResponse(notification);
    }

    @Transactional
    public EmailNotification sendInterviewScheduled(Interview interview, Authentication authentication) {
        Recipient recipient = interviewRecipient(interview);
        EmailTemplate template = template(
                "Interview Scheduled for " + firstNonBlank(interview.getRole(), "Internship Role"),
                """
                        Hello %s,

                        Your interview has been scheduled.

                        Role: %s
                        Interview Type: AI Text Interview
                        Date & Time: %s
                        Duration: 30 minutes

                        Please be available on time.

                        Regards,
                        HR Team
                        """.formatted(
                        recipient.name(),
                        firstNonBlank(interview.getRole(), "Internship Role"),
                        interview.getScheduledAt() == null ? "To be confirmed" : interview.getScheduledAt().format(DATE_TIME_FORMATTER)
                )
        );
        EmailNotification notification = send(recipient.email(), recipient.name(), template, EmailNotificationType.INTERVIEW_SCHEDULED, "Interview", interview.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return notification;
    }

    @Transactional
    public EmailNotificationResponse sendCandidateSelected(Long candidateId, Authentication authentication) {
        Candidate candidate = candidateService.getCandidateOrThrow(candidateId);
        candidate.setStatus(CandidateStatus.SELECTED);
        EmailTemplate template = template(
                "Congratulations, You Have Been Selected",
                """
                        Hello %s,

                        Congratulations. You have been selected for the internship role: %s.
                        Our HR team will share the next steps soon.

                        Regards,
                        HR Team
                        """.formatted(candidate.getName(), candidate.getAppliedRole())
        );
        EmailNotification notification = send(candidate.getEmail(), candidate.getName(), template, EmailNotificationType.CANDIDATE_SELECTED, "Candidate", candidate.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional
    public EmailNotificationResponse sendCandidateRejected(Long candidateId, Authentication authentication) {
        Candidate candidate = candidateService.getCandidateOrThrow(candidateId);
        candidate.setStatus(CandidateStatus.REJECTED);
        EmailTemplate template = template(
                "Update Regarding Your Internship Application",
                """
                        Hello %s,

                        Thank you for applying for the internship role: %s.
                        After reviewing your profile, we regret to inform you that you have not been selected for this role.
                        We appreciate your time and wish you the best.

                        Regards,
                        HR Team
                        """.formatted(candidate.getName(), candidate.getAppliedRole())
        );
        EmailNotification notification = send(candidate.getEmail(), candidate.getName(), template, EmailNotificationType.CANDIDATE_REJECTED, "Candidate", candidate.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional
    public EmailNotificationResponse sendCandidateShortlisted(Long candidateId, Authentication authentication) {
        Candidate candidate = candidateService.getCandidateOrThrow(candidateId);
        candidate.setStatus(CandidateStatus.SHORTLISTED);
        EmailTemplate template = template(
                "You Have Been Shortlisted",
                """
                        Hello %s,

                        You have been shortlisted for the internship role: %s.
                        Our HR team will contact you with the next steps.

                        Regards,
                        HR Team
                        """.formatted(candidate.getName(), candidate.getAppliedRole())
        );
        EmailNotification notification = send(candidate.getEmail(), candidate.getName(), template, EmailNotificationType.CANDIDATE_SHORTLISTED, "Candidate", candidate.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional
    public EmailNotificationResponse sendResumeScreened(Candidate candidate, Authentication authentication) {
        EmailTemplate template = template(
                "Resume Screening Completed",
                """
                        Hello %s,

                        Your resume screening has been completed.

                        Applied Role: %s
                        Current Status: %s
                        Recommendation: %s

                        Regards,
                        HR Team
                        """.formatted(
                        candidate.getName(),
                        candidate.getAppliedRole(),
                        candidate.getStatus(),
                        firstNonBlank(candidate.getAiRecommendation(), "Under review")
                )
        );
        EmailNotification notification = send(candidate.getEmail(), candidate.getName(), template, EmailNotificationType.RESUME_SCREENING_COMPLETED, "Candidate", candidate.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional
    public EmailNotificationResponse sendDepartmentAssignment(Long internId, Authentication authentication) {
        InternProfile intern = internProfileService.getProfileOrThrow(internId);
        EmailTemplate template = departmentTemplate(intern);
        EmailNotification notification = send(intern.getUser().getEmail(), intern.getUser().getName(), template, EmailNotificationType.DEPARTMENT_ASSIGNED, "InternProfile", intern.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional
    public EmailNotificationResponse sendManagerAssignment(Long internId, Authentication authentication) {
        InternProfile intern = internProfileService.getProfileOrThrow(internId);
        EmailTemplate template = template(
                "Manager Assignment Details",
                """
                        Hello %s,

                        Your internship manager has been assigned.

                        Manager: %s
                        Department: %s

                        Please check your intern dashboard for more details.

                        Regards,
                        HR Team
                        """.formatted(intern.getUser().getName(), managerName(intern), departmentName(intern))
        );
        EmailNotification notification = send(intern.getUser().getEmail(), intern.getUser().getName(), template, EmailNotificationType.MANAGER_ASSIGNED, "InternProfile", intern.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional
    public EmailNotificationResponse sendOnboarding(Long internId, Authentication authentication) {
        InternProfile intern = internProfileService.getProfileOrThrow(internId);
        EmailTemplate template = template(
                "Welcome to the Internship Program",
                """
                        Hello %s,

                        Welcome to the internship program.
                        Your intern profile has been created.

                        Emp ID: %s
                        Department: %s
                        Sub Department: %s
                        Assigned Company/Client: %s
                        Manager: %s
                        Start Date: %s

                        Please login to your dashboard for more details.

                        Regards,
                        HR Team
                        """.formatted(
                        intern.getUser().getName(),
                        firstNonBlank(intern.getEmpId(), intern.getUser().getEmpId(), "-"),
                        departmentName(intern),
                        firstNonBlank(intern.getSubDepartment(), "-"),
                        firstNonBlank(intern.getAssignedCompany(), "-"),
                        managerName(intern),
                        intern.getInternshipStartDate() == null ? "To be confirmed" : intern.getInternshipStartDate()
                )
        );
        EmailNotification notification = send(intern.getUser().getEmail(), intern.getUser().getName(), template, EmailNotificationType.INTERN_ONBOARDED, "InternProfile", intern.getId());
        auditLogService.record(actor(authentication), action(notification), "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<EmailNotificationResponse> getNotifications(EmailNotificationType type, EmailNotificationStatus status, String search) {
        return emailNotificationRepository.findAll(specification(type, status, search), org.springframework.data.domain.Sort.by("createdAt").descending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmailNotificationResponse getNotification(Long id) {
        return toResponse(getNotificationOrThrow(id));
    }

    @Transactional
    public EmailNotificationResponse resend(Long id, Authentication authentication) {
        EmailNotification original = getNotificationOrThrow(id);
        EmailTemplate template = template(original.getSubject(), original.getBody());
        EmailNotification notification = send(
                original.getRecipientEmail(),
                original.getRecipientName(),
                template,
                original.getNotificationType(),
                original.getRelatedEntityType(),
                original.getRelatedEntityId()
        );
        auditLogService.record(actor(authentication), action(notification).equals("EMAIL_FAILED") ? "EMAIL_FAILED" : "EMAIL_RESENT", "EmailNotification", notification.getId());
        return toResponse(notification);
    }

    private EmailNotification send(String recipientEmail, String recipientName, EmailTemplate template, EmailNotificationType type, String relatedType, Long relatedId) {
        EmailNotification notification = emailNotificationRepository.save(EmailNotification.builder()
                .recipientEmail(firstNonBlank(recipientEmail, "missing-recipient@interniq.local").trim().toLowerCase(Locale.ROOT))
                .recipientName(firstNonBlank(recipientName, "Recipient"))
                .subject(template.subject())
                .body(template.body())
                .notificationType(type)
                .relatedEntityType(relatedType)
                .relatedEntityId(relatedId)
                .status(EmailNotificationStatus.PENDING)
                .build());

        if (!mailEnabled) {
            notification.setStatus(EmailNotificationStatus.MOCKED);
            notification.setSentAt(LocalDateTime.now());
            log.info("Mock HR email [{}] to {}: {}", type, notification.getRecipientEmail(), template.subject());
            return notification;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(notification.getRecipientEmail());
            message.setSubject(template.subject());
            message.setText(template.body());
            mailSender.send(message);
            notification.setStatus(EmailNotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (MailException ex) {
            notification.setStatus(EmailNotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
            log.error("HR email failed for {}: {}", notification.getRecipientEmail(), ex.getMessage());
            if (failOnError) {
                throw ex;
            }
        }

        return notification;
    }

    private EmailTemplate departmentTemplate(InternProfile intern) {
        return template(
                "Department Assignment Details",
                """
                        Hello %s,

                        You have been assigned to the following department:

                        Department: %s
                        Sub Department: %s
                        Assigned Company/Client: %s
                        Manager: %s

                        Please check your intern dashboard for more details.

                        Regards,
                        HR Team
                        """.formatted(
                        intern.getUser().getName(),
                        departmentName(intern),
                        firstNonBlank(intern.getSubDepartment(), "-"),
                        firstNonBlank(intern.getAssignedCompany(), "-"),
                        managerName(intern)
                )
        );
    }

    private Interview getInterview(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found"));
    }

    private EmailNotification getNotificationOrThrow(Long id) {
        return emailNotificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email notification not found"));
    }

    private Recipient interviewRecipient(Interview interview) {
        if (interview.getIntern() != null) {
            return new Recipient(interview.getIntern().getUser().getEmail(), interview.getIntern().getUser().getName());
        }
        if (interview.getCandidate() != null) {
            return new Recipient(interview.getCandidate().getEmail(), interview.getCandidate().getName());
        }
        throw new IllegalArgumentException("Interview recipient not found");
    }

    private Specification<EmailNotification> specification(EmailNotificationType type, EmailNotificationStatus status, String search) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (type != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("notificationType"), type));
            }
            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("recipientName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("recipientEmail")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("subject")), pattern)
                ));
            }

            return predicate;
        };
    }

    private EmailNotificationResponse toResponse(EmailNotification notification) {
        return EmailNotificationResponse.builder()
                .id(notification.getId())
                .recipientEmail(notification.getRecipientEmail())
                .recipientName(notification.getRecipientName())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .notificationType(notification.getNotificationType())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .status(notification.getStatus())
                .errorMessage(notification.getErrorMessage())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String action(EmailNotification notification) {
        if (notification.getStatus() == EmailNotificationStatus.FAILED) {
            return "EMAIL_FAILED";
        }
        return switch (notification.getNotificationType()) {
            case OFFER_LETTER_SENT -> "OFFER_LETTER_EMAIL_SENT";
            case INTERVIEW_SCHEDULED, INTERVIEW_RESCHEDULED, INTERVIEW_CANCELLED -> "INTERVIEW_EMAIL_SENT";
            case DEPARTMENT_ASSIGNED -> "DEPARTMENT_ASSIGNMENT_EMAIL_SENT";
            case MANAGER_ASSIGNED -> "MANAGER_ASSIGNMENT_EMAIL_SENT";
            case INTERN_ONBOARDED -> "ONBOARDING_EMAIL_SENT";
            default -> notification.getNotificationType().name() + "_EMAIL_SENT";
        };
    }

    private User actor(Authentication authentication) {
        return authentication == null ? null : userService.getCurrentUser(authentication);
    }

    private String departmentName(InternProfile intern) {
        return intern.getDepartment() == null ? "-" : intern.getDepartment().getName();
    }

    private String managerName(InternProfile intern) {
        return intern.getManager() == null ? "-" : intern.getManager().getName();
    }

    private EmailTemplate template(String subject, String body) {
        return new EmailTemplate(subject, body);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private record EmailTemplate(String subject, String body) {
    }

    private record Recipient(String email, String name) {
    }
}
