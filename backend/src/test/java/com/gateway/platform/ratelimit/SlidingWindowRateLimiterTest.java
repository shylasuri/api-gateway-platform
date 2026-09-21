package com.gateway.platform.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import com.gateway.platform.service.ratelimit.RateLimitConfig;
import com.gateway.platform.service.ratelimit.RateLimitDecision;
import com.gateway.platform.service.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlidingWindowRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private SlidingWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        limiter = new SlidingWindowRateLimiter(redisTemplate);
    }

    private RateLimitConfig config(int limit, int windowSeconds) {
        return RateLimitConfig.builder()
                .strategy(RateLimitStrategyType.SLIDING_WINDOW)
                .limit(limit)
                .windowSeconds(windowSeconds)
                .build();
    }

    @Test
    void requestInsideRollingWindow_isAllowed() {
        // script returns {allowed=1, countAfterAdd=10}
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any(), any(), any()))
                .thenReturn(List.of(1L, 10L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(50, 60));

        assertTrue(decision.allowed());
        assertEquals(40, decision.remaining());
    }

    @Test
    void requestExceedingRollingWindowCount_isRejected() {
        // script returns {allowed=0, count=50, retryMs=15000} — 15s until oldest entry expires
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any(), any(), any()))
                .thenReturn(List.of(0L, 50L, 15000L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(50, 60));

        assertFalse(decision.allowed());
        assertEquals(15, decision.retryAfterSeconds());
    }

    @Test
    void getType_returnsSlidingWindow() {
        assertEquals(RateLimitStrategyType.SLIDING_WINDOW, limiter.getType());
    }
}
