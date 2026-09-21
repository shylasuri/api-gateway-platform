package com.gateway.platform.service.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;

/**
 * Common contract for every rate-limiting algorithm. The gateway selects an
 * implementation at runtime, based on RateLimitConfig.strategy() loaded from
 * the database — never a compile-time choice.
 */
public interface RateLimiterStrategy {

    RateLimitStrategyType getType();

    /**
     * @param rateLimitKey unique key identifying the (consumer, API) pair being limited,
     *                      e.g. "rate-limit:{apiKeyId}:{apiId}"
     * @param config        resolved configuration for this key
     */
    RateLimitDecision checkAndRecord(String rateLimitKey, RateLimitConfig config);
}
