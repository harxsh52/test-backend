package com.interniq.token;

import com.interniq.exception.UnauthorizedException;
import com.interniq.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${application.security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken create(User user, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(randomToken())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .deviceInfo(limit(deviceInfo, 500))
                .ipAddress(limit(ipAddress, 80))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotate(String token, String deviceInfo, String ipAddress) {
        RefreshToken existing = validate(token);
        revoke(existing);
        return create(existing.getUser(), deviceInfo, ipAddress);
    }

    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeByToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        refreshTokenRepository.findByToken(token).ifPresent(this::revoke);
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(this::revoke);
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private void revoke(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
    }

    private String randomToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
