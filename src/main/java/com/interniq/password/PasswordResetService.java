package com.interniq.password;

import com.interniq.exception.BadRequestException;
import com.interniq.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long EXPIRY_MINUTES = 30;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public PasswordResetToken create(User user) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(randomToken())
                .expiryDate(Instant.now().plusSeconds(EXPIRY_MINUTES * 60))
                .build();
        PasswordResetToken saved = passwordResetTokenRepository.save(resetToken);
        log.info("Local password reset link for {}: http://localhost:5178/reset-password?token={}", user.getEmail(), saved.getToken());
        return saved;
    }

    @Transactional
    public PasswordResetToken use(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Password reset token is invalid"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("Password reset token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset token has expired");
        }

        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        return resetToken;
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
