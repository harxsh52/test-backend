package com.interniq.leave;

import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.intern.InternProfileService;
import com.interniq.leave.dto.CreateLeaveRequest;
import com.interniq.leave.dto.LeaveBalanceResponse;
import com.interniq.leave.dto.LeaveRequestResponse;
import com.interniq.leave.dto.UpdateLeaveStatusRequest;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final BigDecimal DEFAULT_TOTAL_LEAVES = BigDecimal.valueOf(12);

    private final LeaveRequestRepository leaveRequestRepository;
    private final InternProfileRepository internProfileRepository;
    private final InternProfileService internProfileService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public LeaveRequestResponse createLeave(CreateLeaveRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.INTERN, "Only interns can apply for leave");

        InternProfile profile = internProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));

        validateDateRange(request.getStartDate(), request.getEndDate(), request.getLeaveType());

        if (leaveRequestRepository.existsOverlappingLeave(
                profile.getId(),
                request.getStartDate(),
                request.getEndDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        )) {
            throw new IllegalArgumentException("A leave request already exists for this date range");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .intern(profile)
                .manager(profile.getManager())
                .leaveType(request.getLeaveType())
                .status(LeaveStatus.PENDING)
                .startDate(request.getStartDate())
                .endDate(request.getLeaveType() == LeaveType.HALF_DAY ? request.getStartDate() : request.getEndDate())
                .totalDays(calculateTotalDays(request.getStartDate(), request.getEndDate(), request.getLeaveType()))
                .reason(clean(request.getReason()))
                .build();

        LeaveRequest savedLeave = leaveRequestRepository.save(leaveRequest);
        notifyLeaveCreated(savedLeave);
        return toResponse(savedLeave);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMyLeaves(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.INTERN, "Only interns can view personal leave requests");

        InternProfile profile = internProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));

        return leaveRequestRepository.findByIntern_IdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getManagerLeaves(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.MANAGER, "Only managers can view assigned intern leave requests");

        return leaveRequestRepository.findByManager_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getAllLeaves(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureHrOrAdmin(currentUser, "Only HR and admins can view all leave requests");

        return leaveRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeave(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        LeaveRequest leaveRequest = getLeaveOrThrow(id);
        ensureCanViewLeave(currentUser, leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse updateStatus(Long id, UpdateLeaveStatusRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        LeaveRequest leaveRequest = getLeaveOrThrow(id);
        ensureCanReviewLeave(currentUser, leaveRequest);

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Only pending leave requests can be reviewed");
        }

        if (request.getStatus() != LeaveStatus.APPROVED && request.getStatus() != LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Leave status must be APPROVED or REJECTED");
        }

        leaveRequest.setStatus(request.getStatus());
        leaveRequest.setManagerComment(clean(request.getManagerComment()));
        leaveRequest.setReviewedBy(currentUser);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        notifyLeaveReviewed(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse cancelLeave(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.INTERN, "Only interns can cancel their leave requests");

        LeaveRequest leaveRequest = getLeaveOrThrow(id);

        if (!Objects.equals(leaveRequest.getIntern().getUser().getId(), currentUser.getId())) {
            throw new AccessDeniedException("You can cancel only your own leave request");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Only pending leave requests can be cancelled");
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        return toResponse(leaveRequest);
    }

    @Transactional(readOnly = true)
    public LeaveBalanceResponse getMyBalance(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.INTERN, "Only interns can view personal leave balance");

        InternProfile profile = internProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));

        return buildBalance(profile.getId());
    }

    @Transactional(readOnly = true)
    public LeaveBalanceResponse getInternBalance(Long internId, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        InternProfile profile = internProfileService.getProfileOrThrow(internId);

        if (currentUser.getRole() == Role.MANAGER && !internProfileService.isManagerOf(currentUser, profile)) {
            throw new AccessDeniedException("Managers can view leave balance only for assigned interns");
        }

        if (currentUser.getRole() == Role.INTERN) {
            throw new AccessDeniedException("Interns can use /api/leaves/balance/my");
        }

        if (currentUser.getRole() != Role.MANAGER && currentUser.getRole() != Role.HR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You are not allowed to view this leave balance");
        }

        return buildBalance(profile.getId());
    }

    private LeaveRequest getLeaveOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
    }

    private void ensureCanViewLeave(User currentUser, LeaveRequest leaveRequest) {
        if (currentUser.getRole() == Role.HR || currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER && internProfileService.isManagerOf(currentUser, leaveRequest.getIntern())) {
            return;
        }

        if (currentUser.getRole() == Role.INTERN
                && Objects.equals(leaveRequest.getIntern().getUser().getId(), currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to access this leave request");
    }

    private void ensureCanReviewLeave(User currentUser, LeaveRequest leaveRequest) {
        if (currentUser.getRole() == Role.HR || currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER && internProfileService.isManagerOf(currentUser, leaveRequest.getIntern())) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to review this leave request");
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate, LeaveType leaveType) {
        if (startDate == null || endDate == null) {
            return;
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (leaveType == LeaveType.HALF_DAY && !startDate.equals(endDate)) {
            throw new IllegalArgumentException("Half-day leave must start and end on the same date");
        }
    }

    private BigDecimal calculateTotalDays(LocalDate startDate, LocalDate endDate, LeaveType leaveType) {
        if (leaveType == LeaveType.HALF_DAY) {
            return BigDecimal.valueOf(0.5);
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return BigDecimal.valueOf(days);
    }

    private LeaveBalanceResponse buildBalance(Long internId) {
        BigDecimal usedLeaves = leaveRequestRepository.findByIntern_IdAndStatus(internId, LeaveStatus.APPROVED)
                .stream()
                .map(LeaveRequest::getTotalDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingLeaves = leaveRequestRepository.findByIntern_IdAndStatus(internId, LeaveStatus.PENDING)
                .stream()
                .map(LeaveRequest::getTotalDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return LeaveBalanceResponse.builder()
                .totalLeaves(DEFAULT_TOTAL_LEAVES)
                .usedLeaves(usedLeaves)
                .pendingLeaves(pendingLeaves)
                .remainingLeaves(DEFAULT_TOTAL_LEAVES.subtract(usedLeaves))
                .build();
    }

    private void notifyLeaveCreated(LeaveRequest leaveRequest) {
        if (leaveRequest.getManager() != null) {
            notificationService.createNotification(
                    leaveRequest.getManager(),
                    "New Leave Request",
                    leaveRequest.getIntern().getUser().getName() + " requested leave from "
                            + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate(),
                    NotificationType.LEAVE,
                    NotificationPriority.MEDIUM,
                    "/manager/leaves/" + leaveRequest.getId(),
                    null
            );
        }

        if (leaveRequest.getTotalDays().compareTo(BigDecimal.valueOf(3)) > 0) {
            notificationService.notifyRole(
                    Role.HR,
                    "Long Leave Request",
                    leaveRequest.getIntern().getUser().getName() + " requested " + leaveRequest.getTotalDays() + " days of leave",
                    NotificationType.LEAVE,
                    NotificationPriority.MEDIUM,
                    "/hr/leaves"
            );
        }
    }

    private void notifyLeaveReviewed(LeaveRequest leaveRequest) {
        String decision = leaveRequest.getStatus() == LeaveStatus.APPROVED ? "Approved" : "Rejected";
        notificationService.createNotification(
                leaveRequest.getIntern().getUser(),
                "Leave Request " + decision,
                "Your leave request from " + leaveRequest.getStartDate() + " to "
                        + leaveRequest.getEndDate() + " was " + decision.toLowerCase(),
                NotificationType.LEAVE,
                NotificationPriority.MEDIUM,
                "/intern/leaves/" + leaveRequest.getId(),
                null
        );
    }

    private LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        InternProfile intern = leaveRequest.getIntern();
        User internUser = intern.getUser();
        User manager = leaveRequest.getManager();
        User reviewer = leaveRequest.getReviewedBy();

        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .internId(intern.getId())
                .internUserId(internUser.getId())
                .internName(internUser.getName())
                .internEmail(internUser.getEmail())
                .departmentId(intern.getDepartment() == null ? null : intern.getDepartment().getId())
                .departmentName(intern.getDepartment() == null ? null : intern.getDepartment().getName())
                .managerId(manager == null ? null : manager.getId())
                .managerName(manager == null ? null : manager.getName())
                .leaveType(leaveRequest.getLeaveType())
                .status(leaveRequest.getStatus())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .totalDays(leaveRequest.getTotalDays())
                .reason(leaveRequest.getReason())
                .managerComment(leaveRequest.getManagerComment())
                .reviewedById(reviewer == null ? null : reviewer.getId())
                .reviewedByName(reviewer == null ? null : reviewer.getName())
                .reviewedAt(leaveRequest.getReviewedAt())
                .createdAt(leaveRequest.getCreatedAt())
                .updatedAt(leaveRequest.getUpdatedAt())
                .build();
    }

    private void ensureRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new AccessDeniedException(message);
        }
    }

    private void ensureHrOrAdmin(User user, String message) {
        if (user.getRole() != Role.HR && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(message);
        }
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
