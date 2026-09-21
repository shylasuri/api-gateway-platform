package com.gateway.platform.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import com.gateway.platform.service.ratelimit.FixedWindowRateLimiter;
import com.gateway.platform.service.ratelimit.RateLimitConfig;
import com.gateway.platform.service.ratelimit.RateLimitDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FixedWindowRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private FixedWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        limiter = new FixedWindowRateLimiter(redisTemplate);
    }

    private RateLimitConfig config(int limit, int windowSeconds) {
        return RateLimitConfig.builder()
                .strategy(RateLimitStrategyType.FIXED_WINDOW)
                .limit(limit)
                .windowSeconds(windowSeconds)
                .build();
    }

    @Test
    void requestsWithinLimit_areAllowed() {
        // Redis INCR returns 1..100 for the first 100 calls in the window
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                .thenReturn(List.of(50L, 30L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(100, 60));

        assertTrue(decision.allowed());
        assertEquals(100, decision.limit());
        assertEquals(50, decision.remaining()); // 100 - 50
    }

    @Test
    void requestOverLimit_isRejectedWith429SemanticsAndRetryAfter() {
        // 101st request in a 100/60s window
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                .thenReturn(List.of(101L, 42L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(100, 60));

        assertFalse(decision.allowed());
        assertEquals(100, decision.limit());
        assertEquals(0, decision.remaining());
        assertEquals(42, decision.retryAfterSeconds());
    }

    @Test
    void exactlyAtLimit_isStillAllowed_boundaryCondition() {
        // The 100th request out of 100 must still be allowed
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                .thenReturn(List.of(100L, 55L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(100, 60));

        assertTrue(decision.allowed());
        assertEquals(0, decision.remaining());
    }

    @Test
    void getType_returnsFixedWindow() {
        assertEquals(RateLimitStrategyType.FIXED_WINDOW, limiter.getType());
    }
}
