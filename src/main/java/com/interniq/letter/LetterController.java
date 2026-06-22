package com.interniq.letter;

import com.interniq.common.ApiResponse;
import com.interniq.letter.dto.GenerateLetterRequest;
import com.interniq.letter.dto.LetterResponse;
import com.interniq.letter.dto.UpdateLetterStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/letters")
@RequiredArgsConstructor
public class LetterController {

    private final LetterService letterService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<LetterResponse>> generateLetter(
            @Valid @RequestBody GenerateLetterRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Letter generated successfully", letterService.generateLetter(request, authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<LetterResponse>>> getLetters(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Letters loaded successfully", letterService.getLetters(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LetterResponse>> getLetter(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Letter loaded successfully", letterService.getLetter(id, authentication)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<LetterResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLetterStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Letter status updated successfully", letterService.updateStatus(id, request, authentication)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<LetterResponse>> sendLetter(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Letter sent successfully", letterService.sendLetter(id, authentication)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<String> downloadLetter(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=letter-" + id + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(letterService.downloadHtml(id, authentication));
    }
}
