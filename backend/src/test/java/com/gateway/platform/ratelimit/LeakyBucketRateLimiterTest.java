package com.gateway.platform.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import com.gateway.platform.service.ratelimit.LeakyBucketRateLimiter;
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

class LeakyBucketRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private LeakyBucketRateLimiter limiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        limiter = new LeakyBucketRateLimiter(redisTemplate);
    }

    private RateLimitConfig config(int capacity, int processingRate) {
        return RateLimitConfig.builder()
                .strategy(RateLimitStrategyType.LEAKY_BUCKET)
                .queueCapacity(capacity)
                .processingRate(processingRate)
                .build();
    }

    @Test
    void requestWithSpaceInQueue_isAllowed() {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(List.of(1L, 25L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(50, 5));

        assertTrue(decision.allowed());
        assertEquals(25, decision.remaining());
    }

    @Test
    void fullQueue_rejectsWith429Semantics() {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(List.of(0L, 0L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(50, 5));

        assertFalse(decision.allowed());
        assertTrue(decision.retryAfterSeconds() >= 1);
    }

    @Test
    void getType_returnsLeakyBucket() {
        assertEquals(RateLimitStrategyType.LEAKY_BUCKET, limiter.getType());
    }
}
