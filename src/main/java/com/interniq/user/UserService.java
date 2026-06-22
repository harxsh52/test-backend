package com.interniq.user;

import com.interniq.auth.dto.LoginResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.common.PageResponse;
import com.interniq.user.dto.GeneratedCredentialsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%".toCharArray();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
        }

        throw new IllegalArgumentException("Unsupported authenticated user type");
    }

    @Transactional(readOnly = true)
    public List<LoginResponse.UserInfo> getUsers(Authentication authentication) {
        return getUsers(null, null, authentication);
    }

    @Transactional(readOnly = true)
    public List<LoginResponse.UserInfo> getUsers(Role role, String status, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        ensureHrOrAdmin(currentUser);

        return userRepository.findAll(userSpecification(role, status), org.springframework.data.domain.Sort.by("name").ascending())
                .stream()
                .map(this::toUserInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoginResponse.UserInfo> getUsersByRole(Role role, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        ensureHrOrAdmin(currentUser);

        return userRepository.findByRoleOrderByNameAsc(role)
                .stream()
                .map(this::toUserInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LoginResponse.UserInfo> searchUsers(
            Role role,
            String status,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        ensureHrOrAdmin(currentUser);

        Pageable pageable = PageRequestFactory.create(
                page,
                size,
                sortBy,
                sortDirection,
                Set.of("id", "name", "email", "role", "active", "createdAt", "updatedAt"),
                "name"
        );

        return PageResponse.from(userRepository.findAll(userSpecification(role, status), pageable).map(this::toUserInfo), sortBy, sortDirection);
    }

    @Transactional
    public GeneratedCredentialsResponse generateCredentials(Long userId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        ensureHrOrAdmin(currentUser);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getRole() == Role.HR && target.getRole() == Role.ADMIN) {
            throw new AccessDeniedException("HR users cannot generate admin credentials");
        }

        String temporaryPassword = generateTemporaryPassword();
        target.setPassword(passwordEncoder.encode(temporaryPassword));

        return GeneratedCredentialsResponse.builder()
                .userId(target.getId())
                .name(target.getName())
                .email(target.getEmail())
                .role(target.getRole())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    public LoginResponse.UserInfo toUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .department(user.getDepartment())
                .manager(user.getManagerName())
                .managerName(user.getManagerName())
                .designation(toDesignation(user.getRole()))
                .status(user.isActive() ? "ACTIVE" : "INACTIVE")
                .active(user.isActive())
                .permissions(Permission.forRole(user.getRole()).stream().map(Enum::name).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private void ensureHrOrAdmin(User user) {
        if (user.getRole() != Role.HR && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only HR and admin users can access user management");
        }
    }

    private Specification<User> userSpecification(Role role, String status) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (role != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("role"), role));
            }

            Boolean active = toActive(status);
            if (active != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("active"), active));
            }

            return predicate;
        };
    }

    private Boolean toActive(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVE".equals(normalized) || "TRUE".equals(normalized)) {
            return true;
        }

        if ("INACTIVE".equals(normalized) || "FALSE".equals(normalized)) {
            return false;
        }

        return null;
    }

    private String toDesignation(Role role) {
        return switch (role) {
            case INTERN -> "Intern";
            case MANAGER -> "Manager";
            case HR -> "HR";
            case ADMIN -> "Admin";
        };
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder("IQ-");
        for (int index = 0; index < 10; index += 1) {
            password.append(PASSWORD_CHARS[SECURE_RANDOM.nextInt(PASSWORD_CHARS.length)]);
        }
        return password.toString();
    }
}
