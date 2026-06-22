package com.interniq.audit;

import com.interniq.audit.dto.LoginAuditLogResponse;
import com.interniq.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;

    @Transactional
    public void record(User user, String email, LoginAuditStatus status, String ipAddress, String userAgent, String failureReason) {
        loginAuditRepository.save(LoginAuditLog.builder()
                .user(user)
                .email(email == null ? "unknown" : email.trim().toLowerCase())
                .status(status)
                .ipAddress(limit(ipAddress, 80))
                .userAgent(limit(userAgent, 500))
                .failureReason(limit(failureReason, 500))
                .build());
    }

    @Transactional(readOnly = true)
    public List<LoginAuditLogResponse> latest() {
        return loginAuditRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(log -> LoginAuditLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUser() == null ? null : log.getUser().getId())
                        .userName(log.getUser() == null ? null : log.getUser().getName())
                        .email(log.getEmail())
                        .status(log.getStatus())
                        .ipAddress(log.getIpAddress())
                        .userAgent(log.getUserAgent())
                        .failureReason(log.getFailureReason())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
