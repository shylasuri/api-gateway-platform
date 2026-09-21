package com.gateway.platform.dto.response;

/**
 * Always returns the same generic {@code message} regardless of whether the
 * email matched an account (to avoid account-enumeration). {@code resetToken}
 * and {@code resetUrl} are populated ONLY in local/dev mode
 * (app.password-reset.expose-token=true), where there is no email service to
 * deliver the link; they are null in production.
 */
public record ForgotPasswordResponse(
        String message,
        String resetToken,
        String resetUrl
) {}
