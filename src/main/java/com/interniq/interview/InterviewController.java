package com.interniq.interview;

import com.interniq.common.ApiResponse;
import com.interniq.interview.dto.InterviewAnswerRequest;
import com.interniq.interview.dto.InterviewAnswerResponse;
import com.interniq.interview.dto.InterviewRequest;
import com.interniq.interview.dto.InterviewResponse;
import com.interniq.interview.dto.InterviewResultResponse;
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
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> scheduleInterview(
            @Valid @RequestBody InterviewRequest request,
            Authentication authentication
    ) {
        InterviewResponse response = interviewService.scheduleInterview(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interview scheduled successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getMyInterviews(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Interviews loaded successfully", interviewService.getMyInterviews(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterview(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Interview loaded successfully", interviewService.getInterview(id, authentication)));
    }

    @PostMapping("/{id}/generate-questions")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> generateQuestions(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Interview questions generated successfully", interviewService.generateQuestions(id, authentication)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<InterviewResponse>> startInterview(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Interview started successfully", interviewService.startInterview(id, authentication)));
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<ApiResponse<InterviewAnswerResponse>> submitAnswer(
            @PathVariable Long id,
            @Valid @RequestBody InterviewAnswerRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Answer submitted successfully", interviewService.submitAnswer(id, request, authentication)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<InterviewResponse>> completeInterview(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Interview completed successfully", interviewService.completeInterview(id, authentication)));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResponse<InterviewResultResponse>> getResult(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Interview result loaded successfully", interviewService.getResult(id, authentication)));
    }
}
