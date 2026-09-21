package com.gateway.platform.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Long retryAfter
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message, null);
    }

    public static ErrorResponse of(int status, String error, String message, Long retryAfter) {
        return new ErrorResponse(Instant.now(), status, error, message, retryAfter);
    }
}
