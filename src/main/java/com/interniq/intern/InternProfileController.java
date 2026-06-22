package com.interniq.intern;

import com.interniq.common.ApiResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.intern.dto.InternProfileRequest;
import com.interniq.intern.dto.InternProfileResponse;
import com.interniq.notification.EmailNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interns")
@RequiredArgsConstructor
public class InternProfileController {

    private final InternProfileService internProfileService;
    private final EmailNotificationService emailNotificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getInterns(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Interns loaded successfully",
                    internProfileService.searchInterns(departmentId, managerId, status, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Interns loaded successfully",
                internProfileService.getInterns(departmentId, managerId, status, authentication)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InternProfileResponse>> getIntern(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Intern profile loaded successfully", internProfileService.getIntern(id, authentication)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<InternProfileResponse>> createIntern(
            @Valid @RequestBody InternProfileRequest request,
            Authentication authentication
    ) {
        InternProfileResponse response = internProfileService.createIntern(request);
        emailNotificationService.sendOnboarding(response.getId(), authentication);
        emailNotificationService.sendDepartmentAssignment(response.getId(), authentication);
        emailNotificationService.sendManagerAssignment(response.getId(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Intern profile created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<InternProfileResponse>> updateIntern(
            @PathVariable Long id,
            @Valid @RequestBody InternProfileRequest request,
            Authentication authentication
    ) {
        InternProfileResponse response = internProfileService.updateIntern(id, request);
        emailNotificationService.sendDepartmentAssignment(response.getId(), authentication);
        emailNotificationService.sendManagerAssignment(response.getId(), authentication);
        return ResponseEntity.ok(ApiResponse.success("Intern profile updated successfully", response));
    }

    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<InternProfileResponse>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("My profile loaded successfully", internProfileService.getMyProfile(authentication)));
    }
}
