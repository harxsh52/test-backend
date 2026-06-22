package com.interniq.attendance;

import com.interniq.attendance.dto.AttendanceResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.common.PageResponse;
import com.interniq.common.PagingUtils;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.intern.InternProfileService;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final InternProfileRepository internProfileRepository;
    private final InternProfileService internProfileService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public AttendanceResponse punchIn(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureIntern(currentUser);

        InternProfile profile = getProfileForUser(currentUser);
        LocalDate today = LocalDate.now();

        attendanceRepository.findByIntern_IdAndDate(profile.getId(), today)
                .ifPresent(attendance -> {
                    throw new IllegalArgumentException("You have already punched in today");
                });

        LocalTime punchInTime = LocalTime.now();
        Attendance attendance = Attendance.builder()
                .intern(profile)
                .date(today)
                .punchInTime(punchInTime)
                .totalHours(BigDecimal.ZERO)
                .status("PUNCHED_IN")
                .build();

        // TODO: When the leave policy is finalized, approved leave days can be marked as APPROVED_LEAVE before punch-in.
        Attendance savedAttendance = attendanceRepository.save(attendance);
        notificationService.createNotification(
                currentUser,
                "Punch In Successful",
                "You punched in at " + punchInTime,
                NotificationType.ATTENDANCE,
                NotificationPriority.LOW,
                "/intern/attendance",
                null
        );

        return toResponse(savedAttendance);
    }

    @Transactional
    public AttendanceResponse punchOut(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureIntern(currentUser);

        InternProfile profile = getProfileForUser(currentUser);
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByIntern_IdAndDate(profile.getId(), today)
                .orElseThrow(() -> new IllegalArgumentException("Punch-in is required before punch-out"));

        if (attendance.getPunchOutTime() != null) {
            throw new IllegalArgumentException("You have already punched out today");
        }

        LocalTime punchOutTime = LocalTime.now();
        attendance.setPunchOutTime(punchOutTime);
        attendance.setTotalHours(calculateTotalHours(attendance.getPunchInTime(), punchOutTime));
        attendance.setStatus("PRESENT");
        notificationService.createNotification(
                currentUser,
                "Punch Out Successful",
                "You punched out at " + punchOutTime,
                NotificationType.ATTENDANCE,
                NotificationPriority.LOW,
                "/intern/attendance",
                null
        );

        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance(Authentication authentication) {
        return getMyAttendance(null, null, authentication);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance(LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureIntern(currentUser);

        InternProfile profile = getProfileForUser(currentUser);
        return attendanceRepository.findByIntern_IdOrderByDateDesc(profile.getId())
                .stream()
                .filter(attendance -> withinDateRange(attendance, fromDate, toDate))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> searchMyAttendance(
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        Pageable pageable = attendancePageable(page, size, sortBy, sortDirection);
        return PageResponse.from(PagingUtils.paginate(getMyAttendance(fromDate, toDate, authentication), pageable), sortBy, sortDirection);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceForIntern(Long internId, Authentication authentication) {
        return getAttendanceForIntern(internId, null, null, authentication);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceForIntern(Long internId, LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        InternProfile profile = internProfileService.getProfileOrThrow(internId);
        internProfileService.ensureCanViewProfile(currentUser, profile);

        return attendanceRepository.findByIntern_IdOrderByDateDesc(internId)
                .stream()
                .filter(attendance -> withinDateRange(attendance, fromDate, toDate))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> searchAttendanceForIntern(
            Long internId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        Pageable pageable = attendancePageable(page, size, sortBy, sortDirection);
        return PageResponse.from(PagingUtils.paginate(getAttendanceForIntern(internId, fromDate, toDate, authentication), pageable), sortBy, sortDirection);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendance(Authentication authentication) {
        return getAllAttendance(null, null, authentication);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendance(LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        List<Attendance> attendance = switch (currentUser.getRole()) {
            case ADMIN, HR -> attendanceRepository.findAllByOrderByDateDesc();
            case MANAGER -> attendanceRepository.findByIntern_Manager_IdOrderByDateDesc(currentUser.getId());
            case INTERN -> {
                InternProfile profile = getProfileForUser(currentUser);
                yield attendanceRepository.findByIntern_IdOrderByDateDesc(profile.getId());
            }
        };

        return attendance.stream()
                .filter(record -> withinDateRange(record, fromDate, toDate))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> searchAllAttendance(
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        Pageable pageable = attendancePageable(page, size, sortBy, sortDirection);
        return PageResponse.from(PagingUtils.paginate(getAllAttendance(fromDate, toDate, authentication), pageable), sortBy, sortDirection);
    }

    private InternProfile getProfileForUser(User user) {
        return internProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));
    }

    private void ensureIntern(User user) {
        if (user.getRole() != Role.INTERN) {
            throw new AccessDeniedException("Only interns can perform this action");
        }
    }

    private BigDecimal calculateTotalHours(LocalTime punchInTime, LocalTime punchOutTime) {
        long minutes = Duration.between(punchInTime, punchOutTime).toMinutes();
        if (minutes < 0) {
            minutes += Duration.ofDays(1).toMinutes();
        }
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private boolean withinDateRange(Attendance attendance, LocalDate fromDate, LocalDate toDate) {
        return (fromDate == null || !attendance.getDate().isBefore(fromDate))
                && (toDate == null || !attendance.getDate().isAfter(toDate));
    }

    private Pageable attendancePageable(Integer page, Integer size, String sortBy, String sortDirection) {
        return PageRequestFactory.create(page, size, sortBy, sortDirection, Set.of("id", "date", "status", "totalHours"), "date");
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .internId(attendance.getIntern().getId())
                .internName(attendance.getIntern().getUser().getName())
                .date(attendance.getDate())
                .punchInTime(attendance.getPunchInTime())
                .punchOutTime(attendance.getPunchOutTime())
                .totalHours(attendance.getTotalHours())
                .status(attendance.getStatus())
                .build();
    }
}
