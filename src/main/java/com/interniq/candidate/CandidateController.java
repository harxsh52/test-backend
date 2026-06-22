package com.interniq.candidate;

import com.interniq.candidate.dto.CandidateRequest;
import com.interniq.candidate.dto.CandidateResponse;
import com.interniq.common.ApiResponse;
import com.interniq.common.PageRequestFactory;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getCandidates(
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Candidates loaded successfully",
                    candidateService.searchCandidates(status, role, page, size, sortBy, sortDirection)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success("Candidates loaded successfully", candidateService.getCandidates(status, role)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Candidate loaded successfully", candidateService.getCandidate(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CandidateResponse>> createCandidate(
            @Valid @RequestBody CandidateRequest request,
            Authentication authentication
    ) {
        CandidateResponse response = candidateService.createCandidate(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidate created successfully", response));
    }
}
