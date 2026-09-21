package com.gateway.platform.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final Long retryAfterSeconds;

    public ApiException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, null);
    }

    public ApiException(HttpStatus status, String errorCode, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
}
