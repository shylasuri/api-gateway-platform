package com.gateway.platform.dto.request;

import com.gateway.platform.entity.RateLimitStrategyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RateLimitConfigRequest(
        @NotNull RateLimitStrategyType strategy,
        @Positive int limitCount,
        @Positive int windowSeconds,
        Integer bucketCapacity,
        Integer refillRate,
        Integer queueCapacity,
        Integer processingRate
) {}
