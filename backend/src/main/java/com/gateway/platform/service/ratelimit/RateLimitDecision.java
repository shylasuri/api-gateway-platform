package com.gateway.platform.service.ratelimit;

/**
 * Uniform result returned by every RateLimiterStrategy implementation, regardless
 * of algorithm, so the gateway filter can treat all four strategies identically.
 */
public record RateLimitDecision(
        boolean allowed,
        int limit,
        long remaining,
        long retryAfterSeconds
) {
    public static RateLimitDecision allow(int limit, long remaining) {
        return new RateLimitDecision(true, limit, Math.max(0, remaining), 0);
    }

    public static RateLimitDecision reject(int limit, long retryAfterSeconds) {
        return new RateLimitDecision(false, limit, 0, retryAfterSeconds);
    }
}
