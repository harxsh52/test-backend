package com.interniq.notification;

import com.interniq.common.ApiResponse;
import com.interniq.notification.dto.EmailNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr/notifications")
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
@RequiredArgsConstructor
public class HrNotificationController {

    private final EmailNotificationService emailNotificationService;

    @PostMapping("/send-offer-letter/{candidateId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendOfferLetter(
            @PathVariable Long candidateId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Offer letter email processed successfully", emailNotificationService.sendOfferLetter(candidateId, authentication)));
    }

    @PostMapping("/send-interview-email/{interviewId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendInterviewEmail(
            @PathVariable Long interviewId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Interview email processed successfully", emailNotificationService.sendInterviewEmail(interviewId, authentication)));
    }

    @PostMapping("/send-selected-email/{candidateId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendSelectedEmail(
            @PathVariable Long candidateId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Candidate selected email processed successfully", emailNotificationService.sendCandidateSelected(candidateId, authentication)));
    }

    @PostMapping("/send-rejection-email/{candidateId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendRejectionEmail(
            @PathVariable Long candidateId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Candidate rejection email processed successfully", emailNotificationService.sendCandidateRejected(candidateId, authentication)));
    }

    @PostMapping("/send-shortlisted-email/{candidateId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendShortlistedEmail(
            @PathVariable Long candidateId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Candidate shortlisted email processed successfully", emailNotificationService.sendCandidateShortlisted(candidateId, authentication)));
    }

    @PostMapping("/send-department-assignment/{internId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendDepartmentAssignment(
            @PathVariable Long internId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Department assignment email processed successfully", emailNotificationService.sendDepartmentAssignment(internId, authentication)));
    }

    @PostMapping("/send-manager-assignment/{internId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendManagerAssignment(
            @PathVariable Long internId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Manager assignment email processed successfully", emailNotificationService.sendManagerAssignment(internId, authentication)));
    }

    @PostMapping("/send-onboarding/{internId}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> sendOnboarding(
            @PathVariable Long internId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Intern onboarding email processed successfully", emailNotificationService.sendOnboarding(internId, authentication)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailNotificationResponse>>> getNotifications(
            @RequestParam(required = false) EmailNotificationType type,
            @RequestParam(required = false) EmailNotificationStatus status,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.success("Email notifications loaded successfully", emailNotificationService.getNotifications(type, status, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Email notification loaded successfully", emailNotificationService.getNotification(id)));
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<ApiResponse<EmailNotificationResponse>> resend(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Email notification resent successfully", emailNotificationService.resend(id, authentication)));
    }
}
