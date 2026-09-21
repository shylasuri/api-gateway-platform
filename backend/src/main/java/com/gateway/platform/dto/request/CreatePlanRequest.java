package com.gateway.platform.dto.request;

import com.gateway.platform.entity.RateLimitStrategyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePlanRequest(
        @NotBlank String name,
        @NotNull BigDecimal monthlyPrice,
        @Positive long monthlyRequestQuota,
        Long dailyRequestQuota,
        @Positive int rateLimit,
        int rateLimitWindowSeconds,
        @NotNull RateLimitStrategyType rateLimitStrategy,
        Integer bucketCapacity,
        Integer refillRate,
        Integer queueCapacity,
        Integer processingRate,
        @NotNull BigDecimal overagePricePerRequest
) {}
