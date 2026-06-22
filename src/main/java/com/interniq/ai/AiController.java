package com.interniq.ai;

import com.interniq.ai.dto.ResumeScreeningResponse;
import com.interniq.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping(value = "/resume-screen/{candidateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ResumeScreeningResponse>> screenResume(
            @PathVariable Long candidateId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Resume screened successfully", aiService.screenResume(candidateId, file, authentication)));
    }

    @GetMapping("/resume-screen/{candidateId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ResumeScreeningResponse>> getScreeningResult(@PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success("Resume screening result loaded successfully", aiService.getScreeningResult(candidateId)));
    }
}
