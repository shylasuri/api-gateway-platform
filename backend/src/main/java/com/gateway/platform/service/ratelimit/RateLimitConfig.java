package com.gateway.platform.service.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import lombok.Builder;

/**
 * Immutable, per-request configuration resolved from the database
 * (RateLimitConfiguration override, or falling back to the consumer's SubscriptionPlan).
 * Never hardcoded in Java — always sourced from persisted config.
 */
@Builder
public record RateLimitConfig(
        RateLimitStrategyType strategy,
        int limit,
        int windowSeconds,
        int bucketCapacity,
        int refillRate,
        int queueCapacity,
        int processingRate
) {
}
