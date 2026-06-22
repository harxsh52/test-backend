package com.interniq.attendance;

import com.interniq.attendance.dto.AttendanceResponse;
import com.interniq.common.ApiResponse;
import com.interniq.common.PageRequestFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/punch-in")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> punchIn(Authentication authentication) {
        AttendanceResponse response = attendanceService.punchIn(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Punch-in recorded successfully", response));
    }

    @PostMapping("/punch-out")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> punchOut(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Punch-out recorded successfully", attendanceService.punchOut(authentication)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<?>> getMyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "My attendance loaded successfully",
                    attendanceService.searchMyAttendance(fromDate, toDate, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success("My attendance loaded successfully", attendanceService.getMyAttendance(fromDate, toDate, authentication)));
    }

    @GetMapping("/intern/{internId}")
    public ResponseEntity<ApiResponse<?>> getAttendanceForIntern(
            @PathVariable Long internId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Intern attendance loaded successfully",
                    attendanceService.searchAttendanceForIntern(internId, fromDate, toDate, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success("Intern attendance loaded successfully", attendanceService.getAttendanceForIntern(internId, fromDate, toDate, authentication)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Attendance loaded successfully",
                    attendanceService.searchAllAttendance(fromDate, toDate, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success("Attendance loaded successfully", attendanceService.getAllAttendance(fromDate, toDate, authentication)));
    }
}
