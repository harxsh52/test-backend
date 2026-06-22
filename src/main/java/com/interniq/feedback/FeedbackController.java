package com.interniq.feedback;

import com.interniq.common.ApiResponse;
import com.interniq.feedback.dto.FeedbackRequest;
import com.interniq.feedback.dto.FeedbackResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication
    ) {
        FeedbackResponse response = feedbackService.createFeedback(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feedback created successfully", response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyFeedback(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("My feedback loaded successfully", feedbackService.getMyFeedback(authentication)));
    }

    @GetMapping("/intern/{internId}")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbackForIntern(
            @PathVariable Long internId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Intern feedback loaded successfully", feedbackService.getFeedbackForIntern(internId, authentication)));
    }
}
