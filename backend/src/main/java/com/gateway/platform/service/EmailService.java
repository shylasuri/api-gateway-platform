package com.gateway.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional email via the Resend HTTP API. Follows the same
 * graceful-degradation contract as {@link StripeService}: if no API key is
 * configured, every call becomes a logged no-op rather than an error, so local
 * development and CI work without email credentials.
 */
@Slf4j
@Service
public class EmailService {

    private final String apiKey;
    private final String from;
    private final RestClient restClient;

    public EmailService(
            @Value("${app.mail.resend-api-key:}") String apiKey,
            @Value("${app.mail.from:GatewayPlatform <onboarding@resend.dev>}") String from) {
        this.apiKey = apiKey;
        this.from = from;
        this.restClient = RestClient.create();
        if (isEnabled()) {
            log.info("EmailService enabled (Resend), sending as {}", from);
        } else {
            log.warn("RESEND_API_KEY not set — email sending disabled; reset links will only be logged.");
        }
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void sendPasswordReset(String toEmail, String resetUrl) {
        String subject = "Reset your GatewayPlatform password";
        String html = """
                <div style="font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;max-width:480px;margin:auto">
                  <h2 style="color:#4338ca;margin-bottom:4px">Reset your password</h2>
                  <p style="color:#334155">We received a request to reset your GatewayPlatform password.
                  Click the button below to choose a new one. This link expires shortly and can be used once.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#4f46e5;color:#fff;text-decoration:none;padding:10px 18px;border-radius:8px;font-weight:600">Reset password</a>
                  </p>
                  <p style="color:#64748b;font-size:13px">If you didn't request this, you can safely ignore this email.</p>
                  <p style="color:#94a3b8;font-size:12px;word-break:break-all">%s</p>
                </div>
                """.formatted(resetUrl, resetUrl);
        send(toEmail, subject, html);
    }

    private void send(String toEmail, String subject, String html) {
        if (!isEnabled()) {
            log.info("[email disabled] would send '{}' to {}", subject, toEmail);
            return;
        }
        try {
            restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", from,
                            "to", List.of(toEmail),
                            "subject", subject,
                            "html", html))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent '{}' to {}", subject, toEmail);
        } catch (Exception e) {
            // Never surface email-provider failures to the caller — the reset token
            // is already persisted, and we don't want to reveal delivery outcomes.
            log.error("Failed to send email '{}' to {}: {}", subject, toEmail, e.getMessage());
        }
    }
}
