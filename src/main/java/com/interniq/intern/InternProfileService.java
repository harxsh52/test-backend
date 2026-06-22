package com.interniq.intern;

import com.interniq.common.PageRequestFactory;
import com.interniq.common.PageResponse;
import com.interniq.common.PagingUtils;
import com.interniq.department.Department;
import com.interniq.department.DepartmentRepository;
import com.interniq.intern.dto.InternProfileRequest;
import com.interniq.intern.dto.InternProfileResponse;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InternProfileService {

    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<InternProfileResponse> getInterns(Authentication authentication) {
        return getInterns(null, null, null, authentication);
    }

    @Transactional(readOnly = true)
    public List<InternProfileResponse> getInterns(Long departmentId, Long managerId, String status, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        return filterProfiles(getAccessibleProfiles(currentUser), departmentId, managerId, status)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InternProfileResponse> searchInterns(
            Long departmentId,
            Long managerId,
            String status,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        User currentUser = userService.getCurrentUser(authentication);
        Pageable pageable = PageRequestFactory.create(
                page,
                size,
                sortBy,
                sortDirection,
                java.util.Set.of("id", "status", "joiningDate", "internshipStartDate", "internshipEndDate"),
                "id"
        );

        List<InternProfileResponse> records = filterProfiles(getAccessibleProfiles(currentUser), departmentId, managerId, status)
                .map(this::toResponse)
                .toList();

        return PageResponse.from(PagingUtils.paginate(records, pageable), sortBy, sortDirection);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getIntern(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        InternProfile profile = getProfileOrThrow(id);
        ensureCanViewProfile(currentUser, profile);
        return toResponse(profile);
    }

    @Transactional
    public InternProfileResponse createIntern(InternProfileRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (internProfileRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("Intern profile already exists for this user");
        }

        User internUser = getUserOrThrow(request.getUserId());
        ensureRole(internUser, Role.INTERN, "Selected user must have INTERN role");

        InternProfile profile = InternProfile.builder()
                .user(internUser)
                .department(getDepartment(request.getDepartmentId()))
                .manager(getManager(request.getManagerId()))
                .phone(clean(request.getPhone()))
                .college(clean(request.getCollege()))
                .skills(clean(request.getSkills()))
                .joiningDate(request.getJoiningDate())
                .internshipStartDate(request.getInternshipStartDate())
                .internshipEndDate(request.getInternshipEndDate())
                .status(defaultStatus(request.getStatus()))
                .build();

        return toResponse(internProfileRepository.save(profile));
    }

    @Transactional
    public InternProfileResponse updateIntern(Long id, InternProfileRequest request) {
        InternProfile profile = getProfileOrThrow(id);

        if (request.getUserId() != null && !request.getUserId().equals(profile.getUser().getId())) {
            throw new IllegalArgumentException("Intern user cannot be changed after profile creation");
        }

        profile.setDepartment(getDepartment(request.getDepartmentId()));
        profile.setManager(getManager(request.getManagerId()));
        profile.setPhone(clean(request.getPhone()));
        profile.setCollege(clean(request.getCollege()));
        profile.setSkills(clean(request.getSkills()));
        profile.setJoiningDate(request.getJoiningDate());
        profile.setInternshipStartDate(request.getInternshipStartDate());
        profile.setInternshipEndDate(request.getInternshipEndDate());
        profile.setStatus(defaultStatus(request.getStatus()));

        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getMyProfile(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        ensureRole(currentUser, Role.INTERN, "Only interns have a personal intern profile");

        InternProfile profile = internProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));

        return toResponse(profile);
    }

    public InternProfile getProfileOrThrow(Long id) {
        return internProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Intern profile not found"));
    }

    public boolean isManagerOf(User manager, InternProfile profile) {
        return profile.getManager() != null && Objects.equals(profile.getManager().getId(), manager.getId());
    }

    public void ensureCanViewProfile(User currentUser, InternProfile profile) {
        if (isHrOrAdmin(currentUser)) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER && isManagerOf(currentUser, profile)) {
            return;
        }

        if (currentUser.getRole() == Role.INTERN && Objects.equals(profile.getUser().getId(), currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to access this intern profile");
    }

    public InternProfileResponse toResponse(InternProfile profile) {
        Department department = profile.getDepartment();
        User manager = profile.getManager();

        return InternProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .email(profile.getUser().getEmail())
                .departmentId(department == null ? null : department.getId())
                .departmentName(department == null ? null : department.getName())
                .managerId(manager == null ? null : manager.getId())
                .managerName(manager == null ? null : manager.getName())
                .phone(profile.getPhone())
                .college(profile.getCollege())
                .skills(profile.getSkills())
                .joiningDate(profile.getJoiningDate())
                .internshipStartDate(profile.getInternshipStartDate())
                .internshipEndDate(profile.getInternshipEndDate())
                .status(profile.getStatus())
                .build();
    }

    private List<InternProfile> getAccessibleProfiles(User currentUser) {
        return switch (currentUser.getRole()) {
            case ADMIN, HR -> internProfileRepository.findAll();
            case MANAGER -> internProfileRepository.findByManager_Id(currentUser.getId());
            case INTERN -> internProfileRepository.findByUserId(currentUser.getId())
                    .map(List::of)
                    .orElseGet(List::of);
        };
    }

    private java.util.stream.Stream<InternProfile> filterProfiles(
            List<InternProfile> profiles,
            Long departmentId,
            Long managerId,
            String status
    ) {
        return profiles.stream()
                .filter(profile -> departmentId == null
                        || (profile.getDepartment() != null && Objects.equals(profile.getDepartment().getId(), departmentId)))
                .filter(profile -> managerId == null
                        || (profile.getManager() != null && Objects.equals(profile.getManager().getId(), managerId)))
                .filter(profile -> status == null || status.isBlank() || status.equalsIgnoreCase(profile.getStatus()))
                .sorted(Comparator.comparing(profile -> profile.getUser().getName(), String.CASE_INSENSITIVE_ORDER));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Department getDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }

        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    private User getManager(Long managerId) {
        if (managerId == null) {
            return null;
        }

        User manager = getUserOrThrow(managerId);
        ensureRole(manager, Role.MANAGER, "Selected manager must have MANAGER role");
        return manager;
    }

    private void ensureRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isHrOrAdmin(User user) {
        return user.getRole() == Role.HR || user.getRole() == Role.ADMIN;
    }

    private String defaultStatus(String status) {
        String cleaned = clean(status);
        return cleaned == null || cleaned.isBlank() ? "ACTIVE" : cleaned;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
