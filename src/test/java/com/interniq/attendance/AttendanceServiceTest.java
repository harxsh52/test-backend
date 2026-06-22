package com.interniq.attendance;

import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.intern.InternProfileService;
import com.interniq.notification.NotificationService;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private InternProfileService internProfileService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void punchInSuccessfully() {
        User internUser = user(1L, Role.INTERN);
        InternProfile profile = profile(10L, internUser, null);
        Authentication authentication = authentication(internUser);

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(internProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(attendanceRepository.findByIntern_IdAndDate(10L, LocalDate.now())).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setId(100L);
            return attendance;
        });

        var response = attendanceService.punchIn(authentication);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getInternId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("PUNCHED_IN");
        assertThat(response.getPunchInTime()).isNotNull();
    }

    @Test
    void preventDoublePunchInOnSameDay() {
        User internUser = user(1L, Role.INTERN);
        InternProfile profile = profile(10L, internUser, null);
        Authentication authentication = authentication(internUser);

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(internProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(attendanceRepository.findByIntern_IdAndDate(10L, LocalDate.now()))
                .thenReturn(Optional.of(attendance(100L, profile)));

        assertThatThrownBy(() -> attendanceService.punchIn(authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already punched in");
    }

    @Test
    void preventPunchOutWithoutPunchIn() {
        User internUser = user(1L, Role.INTERN);
        InternProfile profile = profile(10L, internUser, null);
        Authentication authentication = authentication(internUser);

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(internProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(attendanceRepository.findByIntern_IdAndDate(10L, LocalDate.now())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.punchOut(authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Punch-in is required");
    }

    @Test
    void punchOutSuccessfullyAndCalculateTotalWorkingHours() {
        User internUser = user(1L, Role.INTERN);
        InternProfile profile = profile(10L, internUser, null);
        Authentication authentication = authentication(internUser);
        Attendance attendance = attendance(100L, profile);
        attendance.setPunchInTime(LocalTime.now().minusHours(8));

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(internProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(attendanceRepository.findByIntern_IdAndDate(10L, LocalDate.now())).thenReturn(Optional.of(attendance));

        var response = attendanceService.punchOut(authentication);

        assertThat(response.getStatus()).isEqualTo("PRESENT");
        assertThat(response.getPunchOutTime()).isNotNull();
        assertThat(response.getTotalHours()).isGreaterThanOrEqualTo(BigDecimal.valueOf(7.99));
    }

    @Test
    void internCanViewOwnAttendance() {
        User internUser = user(1L, Role.INTERN);
        InternProfile profile = profile(10L, internUser, null);
        Authentication authentication = authentication(internUser);

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(internProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(attendanceRepository.findByIntern_IdOrderByDateDesc(10L)).thenReturn(List.of(attendance(100L, profile)));

        var records = attendanceService.getMyAttendance(authentication);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getInternId()).isEqualTo(10L);
    }

    @Test
    void adminAndHrCanViewAllAttendance() {
        User admin = user(4L, Role.ADMIN);
        User hr = user(3L, Role.HR);
        InternProfile profile = profile(10L, user(1L, Role.INTERN), null);
        Authentication adminAuthentication = authentication(admin);
        Authentication hrAuthentication = authentication(hr);

        when(userService.getCurrentUser(adminAuthentication)).thenReturn(admin);
        when(userService.getCurrentUser(hrAuthentication)).thenReturn(hr);
        when(attendanceRepository.findAllByOrderByDateDesc()).thenReturn(List.of(attendance(100L, profile)));

        assertThat(attendanceService.getAllAttendance(adminAuthentication)).hasSize(1);
        assertThat(attendanceService.getAllAttendance(hrAuthentication)).hasSize(1);
    }

    @Test
    void nonInternCannotPunchIn() {
        User manager = user(2L, Role.MANAGER);
        Authentication authentication = authentication(manager);

        when(userService.getCurrentUser(authentication)).thenReturn(manager);

        assertThatThrownBy(() -> attendanceService.punchIn(authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only interns");
    }

    private Attendance attendance(Long id, InternProfile profile) {
        return Attendance.builder()
                .id(id)
                .intern(profile)
                .date(LocalDate.now())
                .punchInTime(LocalTime.now().minusHours(2))
                .totalHours(BigDecimal.ZERO)
                .status("PUNCHED_IN")
                .build();
    }

    private Authentication authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private InternProfile profile(Long id, User user, User manager) {
        return InternProfile.builder()
                .id(id)
                .user(user)
                .manager(manager)
                .status("ACTIVE")
                .build();
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .name(role.name())
                .email(role.name().toLowerCase() + "@test.com")
                .password("encoded")
                .role(role)
                .active(true)
                .build();
    }
}
