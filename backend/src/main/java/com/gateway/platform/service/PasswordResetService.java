package com.gateway.platform.service;

import com.gateway.platform.dto.response.ForgotPasswordResponse;
import com.gateway.platform.entity.PasswordResetToken;
import com.gateway.platform.entity.User;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.repository.PasswordResetTokenRepository;
import com.gateway.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Token-based password reset. Tokens are random, hashed at rest (only the
 * SHA-256 hash is stored), single-use, and time-limited. There is no email
 * service in this project, so the reset link is always logged and — only when
 * app.password-reset.expose-token=true (local/dev) — returned in the API
 * response so the flow is testable without email infrastructure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.password-reset.token-expiry-minutes:30}")
    private long expiryMinutes;

    @Value("${app.password-reset.expose-token:false}")
    private boolean exposeToken;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final String GENERIC_MESSAGE =
            "If an account exists for that email, a password reset link has been sent.";

    /**
     * Issues a reset token for the email if an account exists. Always returns the
     * same generic message either way, so callers can't probe which emails are
     * registered.
     */
    @Transactional
    public ForgotPasswordResponse requestReset(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new ForgotPasswordResponse(GENERIC_MESSAGE, null, null);
        }
        User user = userOpt.get();

        // Invalidate any still-active tokens for this user so only the newest works.
        List<PasswordResetToken> active = tokenRepository.findByUserIdAndUsedFalse(user.getId());
        active.forEach(t -> t.setUsed(true));
        if (!active.isEmpty()) {
            tokenRepository.saveAll(active);
        }

        String rawToken = generateToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(expiryMinutes)))
                .used(false)
                .build();
        tokenRepository.save(token);

        String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;
        log.info("Password reset requested for {} — reset link (valid {} min): {}", email, expiryMinutes, resetUrl);
        emailService.sendPasswordReset(user.getEmail(), resetUrl);

        return exposeToken
                ? new ForgotPasswordResponse(GENERIC_MESSAGE, rawToken, resetUrl)
                : new ForgotPasswordResponse(GENERIC_MESSAGE, null, null);
    }

    /** Consumes a valid token and sets the new (BCrypt-hashed) password. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> !t.isUsed() && !t.isExpired())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN",
                        "This password reset link is invalid or has expired. Please request a new one."));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
        log.info("Password reset completed for {}", user.getEmail());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
