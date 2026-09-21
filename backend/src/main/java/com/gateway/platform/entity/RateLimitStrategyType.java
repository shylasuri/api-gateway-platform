package com.gateway.platform.entity;

public enum RateLimitStrategyType {
    FIXED_WINDOW,
    SLIDING_WINDOW,
    TOKEN_BUCKET,
    LEAKY_BUCKET
}
