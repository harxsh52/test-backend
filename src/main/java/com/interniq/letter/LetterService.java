package com.interniq.letter;

import com.interniq.candidate.CandidateRepository;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileService;
import com.interniq.letter.dto.GenerateLetterRequest;
import com.interniq.letter.dto.LetterResponse;
import com.interniq.letter.dto.UpdateLetterStatusRequest;
import com.interniq.notification.EmailService;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LetterService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LetterRepository letterRepository;
    private final CandidateRepository candidateRepository;
    private final InternProfileService internProfileService;
    private final UserService userService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public LetterResponse generateLetter(GenerateLetterRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureHrOrAdmin(currentUser);
        validateRequest(request);

        EnrichedRequest enriched = enrichRequest(request);
        String subject = subjectFor(enriched);
        String bodyText = bodyTextFor(enriched);

        Letter letter = Letter.builder()
                .candidateName(clean(enriched.candidateName()))
                .candidateEmail(clean(enriched.candidateEmail()))
                .candidateId(enriched.candidateId())
                .internId(enriched.internId())
                .letterType(enriched.letterType())
                .status(LetterStatus.GENERATED)
                .roleName(clean(enriched.roleName()))
                .department(clean(enriched.department()))
                .joiningDate(enriched.joiningDate())
                .internshipStartDate(enriched.internshipStartDate())
                .internshipEndDate(enriched.internshipEndDate())
                .stipend(enriched.stipend())
                .workLocation(clean(enriched.workLocation()))
                .reportingManager(clean(enriched.reportingManager()))
                .companyName(clean(enriched.companyName()))
                .hrName(clean(enriched.hrName()))
                .subject(subject)
                .bodyText(bodyText)
                .bodyHtml(toHtml(subject, bodyText, enriched.companyName()))
                .generatedBy(currentUser)
                .build();

        return toResponse(letterRepository.save(letter));
    }

    @Transactional(readOnly = true)
    public List<LetterResponse> getLetters(Authentication authentication) {
        ensureHrOrAdmin(userService.getCurrentUser(authentication));
        return letterRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LetterResponse getLetter(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Letter letter = getLetterOrThrow(id);
        ensureCanView(currentUser, letter);
        return toResponse(letter);
    }

    @Transactional
    public LetterResponse updateStatus(Long id, UpdateLetterStatusRequest request, Authentication authentication) {
        ensureHrOrAdmin(userService.getCurrentUser(authentication));
        Letter letter = getLetterOrThrow(id);
        letter.setStatus(request.getStatus());
        LocalDateTime now = LocalDateTime.now();

        if (request.getStatus() == LetterStatus.SENT && letter.getSentAt() == null) {
            letter.setSentAt(now);
        } else if (request.getStatus() == LetterStatus.ACCEPTED) {
            letter.setAcceptedAt(now);
        } else if (request.getStatus() == LetterStatus.REJECTED) {
            letter.setRejectedAt(now);
        }

        return toResponse(letter);
    }

    @Transactional
    public LetterResponse sendLetter(Long id, Authentication authentication) {
        ensureHrOrAdmin(userService.getCurrentUser(authentication));
        Letter letter = getLetterOrThrow(id);
        emailService.sendLetter(letter.getCandidateEmail(), letter.getSubject(), letter.getBodyText());
        letter.setStatus(LetterStatus.SENT);
        letter.setSentAt(LocalDateTime.now());
        notifyInternIfPresent(letter);
        return toResponse(letter);
    }

    @Transactional(readOnly = true)
    public String downloadHtml(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Letter letter = getLetterOrThrow(id);
        ensureCanView(currentUser, letter);
        return letter.getBodyHtml();
    }

    private Letter getLetterOrThrow(Long id) {
        return letterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Letter not found"));
    }

    private void validateRequest(GenerateLetterRequest request) {
        if (request.getLetterType() == LetterType.OFFER_LETTER
                && (request.getJoiningDate() == null || request.getInternshipStartDate() == null || request.getInternshipEndDate() == null)) {
            throw new IllegalArgumentException("Offer letters require joining date, internship start date, and internship end date");
        }

        if (request.getInternshipStartDate() != null
                && request.getInternshipEndDate() != null
                && request.getInternshipStartDate().isAfter(request.getInternshipEndDate())) {
            throw new IllegalArgumentException("Internship start date cannot be after end date");
        }
    }

    private EnrichedRequest enrichRequest(GenerateLetterRequest request) {
        String candidateName = request.getCandidateName();
        String candidateEmail = request.getCandidateEmail();
        String roleName = request.getRoleName();
        String department = request.getDepartment();
        String reportingManager = request.getReportingManager();

        if (request.getCandidateId() != null) {
            var candidate = candidateRepository.findById(request.getCandidateId())
                    .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
            candidateName = firstNonBlank(candidateName, candidate.getName());
            candidateEmail = firstNonBlank(candidateEmail, candidate.getEmail());
            roleName = firstNonBlank(roleName, candidate.getAppliedRole());
        }

        if (request.getInternId() != null) {
            InternProfile intern = internProfileService.getProfileOrThrow(request.getInternId());
            candidateName = firstNonBlank(candidateName, intern.getUser().getName());
            candidateEmail = firstNonBlank(candidateEmail, intern.getUser().getEmail());
            department = firstNonBlank(department, intern.getDepartment() == null ? null : intern.getDepartment().getName());
            reportingManager = firstNonBlank(reportingManager, intern.getManager() == null ? null : intern.getManager().getName());
            roleName = firstNonBlank(roleName, intern.getDesignation());
        }

        return new EnrichedRequest(
                candidateName,
                candidateEmail,
                request.getCandidateId(),
                request.getInternId(),
                request.getLetterType(),
                firstNonBlank(roleName, "Intern"),
                firstNonBlank(department, "General"),
                request.getJoiningDate(),
                request.getInternshipStartDate(),
                request.getInternshipEndDate(),
                request.getStipend(),
                firstNonBlank(request.getWorkLocation(), "Remote"),
                firstNonBlank(reportingManager, "Reporting Manager"),
                firstNonBlank(request.getCompanyName(), "InternIQ Technologies"),
                firstNonBlank(request.getHrName(), "HR Team")
        );
    }

    private String subjectFor(EnrichedRequest request) {
        return switch (request.letterType()) {
            case OFFER_LETTER -> "Internship Offer Letter - " + request.roleName();
            case SELECTION_LETTER -> "Congratulations! You Have Been Selected for Internship";
            case REJECTION_LETTER -> "Update on Your Internship Application";
            case COMPLETION_CERTIFICATE -> "Internship Completion Certificate - " + request.candidateName();
            case EXPERIENCE_LETTER -> "Experience Letter - " + request.candidateName();
        };
    }

    private String bodyTextFor(EnrichedRequest request) {
        return switch (request.letterType()) {
            case OFFER_LETTER -> offerLetter(request);
            case SELECTION_LETTER -> selectionLetter(request);
            case REJECTION_LETTER -> rejectionLetter(request);
            case COMPLETION_CERTIFICATE -> completionCertificate(request);
            case EXPERIENCE_LETTER -> experienceLetter(request);
        };
    }

    private String offerLetter(EnrichedRequest request) {
        return """
                Dear %s,

                We are pleased to offer you the position of %s Intern in the %s department at %s.

                Your internship details are as follows:

                Role: %s
                Department: %s
                Internship Start Date: %s
                Internship End Date: %s
                Joining Date: %s
                Work Location: %s
                Reporting Manager: %s
                Stipend: %s per month

                During your internship, you will be expected to follow company policies, maintain professionalism, complete assigned tasks, and actively participate in learning and project work.

                Please confirm your acceptance of this offer.

                Best regards,
                %s
                %s
                """.formatted(request.candidateName(), request.roleName(), request.department(), request.companyName(),
                request.roleName(), request.department(), formatDate(request.internshipStartDate()), formatDate(request.internshipEndDate()),
                formatDate(request.joiningDate()), request.workLocation(), request.reportingManager(), formatStipend(request.stipend()),
                request.hrName(), request.companyName());
    }

    private String selectionLetter(EnrichedRequest request) {
        return """
                Dear %s,

                Congratulations!

                We are happy to inform you that you have been selected for the %s Internship position at %s.

                You have been selected based on your profile, interview performance, and overall suitability for the role.

                Your internship details will be shared with you shortly.

                We look forward to having you onboard.

                Best regards,
                %s
                %s
                """.formatted(request.candidateName(), request.roleName(), request.companyName(), request.hrName(), request.companyName());
    }

    private String rejectionLetter(EnrichedRequest request) {
        return """
                Dear %s,

                Thank you for applying for the %s Internship position at %s.

                After careful review of your profile and interview performance, we regret to inform you that we will not be moving forward with your application at this time.

                We appreciate your interest and the time you invested in the process. We encourage you to continue improving your skills and apply again in the future.

                We wish you all the best in your career journey.

                Best regards,
                %s
                %s
                """.formatted(request.candidateName(), request.roleName(), request.companyName(), request.hrName(), request.companyName());
    }

    private String completionCertificate(EnrichedRequest request) {
        return """
                This is to certify that %s has successfully completed the %s Internship in the %s department at %s.

                Internship Duration: %s to %s

                Performance Summary:
                Performance summary will be added after final manager review and HR approval.

                We appreciate the contribution made during the internship period and wish %s continued success.

                Best regards,
                %s
                %s
                """.formatted(request.candidateName(), request.roleName(), request.department(), request.companyName(),
                formatDate(request.internshipStartDate()), formatDate(request.internshipEndDate()), request.candidateName(),
                request.hrName(), request.companyName());
    }

    private String experienceLetter(EnrichedRequest request) {
        return """
                This letter confirms that %s worked as a %s Intern in the %s department at %s.

                Duration: %s to %s

                Responsibilities:
                Responsibilities and project details will be added from completed tasks and final feedback.

                Performance Summary:
                Performance summary placeholder.

                Best regards,
                %s
                %s
                """.formatted(request.candidateName(), request.roleName(), request.department(), request.companyName(),
                formatDate(request.internshipStartDate()), formatDate(request.internshipEndDate()), request.hrName(), request.companyName());
    }

    private String toHtml(String subject, String bodyText, String companyName) {
        String paragraphs = bodyText.lines()
                .map(line -> line.isBlank() ? "<br/>" : "<p>" + escapeHtml(line) + "</p>")
                .reduce("", String::concat);

        return """
                <!doctype html>
                <html>
                <head><meta charset="utf-8"><title>%s</title></head>
                <body style="font-family: Arial, sans-serif; background:#f5f7fb; padding:32px;">
                  <main style="max-width:760px;margin:auto;background:#fff;border:1px solid #e5e7eb;padding:40px;">
                    <h1 style="margin:0 0 8px;">%s</h1>
                    <p style="color:#64748b;margin:0 0 24px;">%s</p>
                    <h2 style="font-size:20px;">%s</h2>
                    <section style="line-height:1.65;color:#111827;">%s</section>
                  </main>
                </body>
                </html>
                """.formatted(escapeHtml(subject), escapeHtml(companyName), formatDate(LocalDate.now()), escapeHtml(subject), paragraphs);
    }

    private void notifyInternIfPresent(Letter letter) {
        if (letter.getInternId() == null) {
            return;
        }

        try {
            InternProfile intern = internProfileService.getProfileOrThrow(letter.getInternId());
            notificationService.createNotification(
                    intern.getUser(),
                    "Internship Letter Sent",
                    "Your " + letter.getLetterType().name().replace("_", " ") + " has been sent.",
                    NotificationType.USER_MANAGEMENT,
                    NotificationPriority.HIGH,
                    "/notifications",
                    null
            );
        } catch (RuntimeException ignored) {
            // Email/mock email remains successful even if the optional in-app notification cannot be created.
        }
    }

    private void ensureCanView(User currentUser, Letter letter) {
        if (currentUser.getRole() == Role.HR || currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (letter.getInternId() != null) {
            InternProfile intern = internProfileService.getProfileOrThrow(letter.getInternId());
            if (intern.getUser().getId().equals(currentUser.getId())) {
                return;
            }
        }

        throw new AccessDeniedException("You are not allowed to access this letter");
    }

    private void ensureHrOrAdmin(User user) {
        if (user.getRole() != Role.HR && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only HR and admins can manage letters");
        }
    }

    private LetterResponse toResponse(Letter letter) {
        User generatedBy = letter.getGeneratedBy();
        return LetterResponse.builder()
                .id(letter.getId())
                .candidateName(letter.getCandidateName())
                .candidateEmail(letter.getCandidateEmail())
                .candidateId(letter.getCandidateId())
                .internId(letter.getInternId())
                .letterType(letter.getLetterType())
                .status(letter.getStatus())
                .roleName(letter.getRoleName())
                .department(letter.getDepartment())
                .joiningDate(letter.getJoiningDate())
                .internshipStartDate(letter.getInternshipStartDate())
                .internshipEndDate(letter.getInternshipEndDate())
                .stipend(letter.getStipend())
                .workLocation(letter.getWorkLocation())
                .reportingManager(letter.getReportingManager())
                .companyName(letter.getCompanyName())
                .hrName(letter.getHrName())
                .subject(letter.getSubject())
                .bodyHtml(letter.getBodyHtml())
                .bodyText(letter.getBodyText())
                .generatedById(generatedBy == null ? null : generatedBy.getId())
                .generatedByName(generatedBy == null ? null : generatedBy.getName())
                .sentAt(letter.getSentAt())
                .acceptedAt(letter.getAcceptedAt())
                .rejectedAt(letter.getRejectedAt())
                .createdAt(letter.getCreatedAt())
                .updatedAt(letter.getUpdatedAt())
                .build();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred.trim();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "To be confirmed" : date.format(DATE_FORMATTER);
    }

    private String formatStipend(BigDecimal stipend) {
        return stipend == null ? "As discussed" : "Rs. " + stipend.stripTrailingZeros().toPlainString();
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private record EnrichedRequest(
            String candidateName,
            String candidateEmail,
            Long candidateId,
            Long internId,
            LetterType letterType,
            String roleName,
            String department,
            LocalDate joiningDate,
            LocalDate internshipStartDate,
            LocalDate internshipEndDate,
            BigDecimal stipend,
            String workLocation,
            String reportingManager,
            String companyName,
            String hrName
    ) {
    }
}
