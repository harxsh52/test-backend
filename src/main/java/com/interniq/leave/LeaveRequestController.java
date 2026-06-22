package com.interniq.leave;

import com.interniq.common.ApiResponse;
import com.interniq.leave.dto.CreateLeaveRequest;
import com.interniq.leave.dto.LeaveBalanceResponse;
import com.interniq.leave.dto.LeaveRequestResponse;
import com.interniq.leave.dto.UpdateLeaveStatusRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createLeave(
            @Valid @RequestBody CreateLeaveRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave request created successfully", leaveRequestService.createLeave(request, authentication)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getMyLeaves(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Leave requests loaded successfully", leaveRequestService.getMyLeaves(authentication)));
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getManagerLeaves(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Manager leave requests loaded successfully", leaveRequestService.getManagerLeaves(authentication)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getAllLeaves(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("All leave requests loaded successfully", leaveRequestService.getAllLeaves(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getLeave(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Leave request loaded successfully", leaveRequestService.getLeave(id, authentication)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeaveStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Leave request reviewed successfully", leaveRequestService.updateStatus(id, request, authentication)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancelLeave(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Leave request cancelled successfully", leaveRequestService.cancelLeave(id, authentication)));
    }

    @GetMapping("/balance/my")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getMyBalance(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Leave balance loaded successfully", leaveRequestService.getMyBalance(authentication)));
    }

    @GetMapping("/balance/{internId}")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN')")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getInternBalance(
            @PathVariable Long internId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Leave balance loaded successfully", leaveRequestService.getInternBalance(internId, authentication)));
    }
}
