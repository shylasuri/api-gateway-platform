package com.gateway.platform.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import com.gateway.platform.service.ratelimit.RateLimitConfig;
import com.gateway.platform.service.ratelimit.RateLimitDecision;
import com.gateway.platform.service.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenBucketRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private TokenBucketRateLimiter limiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        limiter = new TokenBucketRateLimiter(redisTemplate);
    }

    private RateLimitConfig config(int capacity, int refillRate) {
        return RateLimitConfig.builder()
                .strategy(RateLimitStrategyType.TOKEN_BUCKET)
                .bucketCapacity(capacity)
                .refillRate(refillRate)
                .build();
    }

    @Test
    void requestWithTokensAvailable_isAllowedAndConsumesOneToken() {
        // capacity=100, refillRate=10/s; bucket has 55 tokens left after consuming 1
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(List.of(1L, 55L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(100, 10));

        assertTrue(decision.allowed());
        assertEquals(100, decision.limit());
        assertEquals(55, decision.remaining());
    }

    @Test
    void requestWithNoTokensAvailable_isRejected() {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(List.of(0L, 0L));

        RateLimitDecision decision = limiter.checkAndRecord("key1", config(100, 10));

        assertFalse(decision.allowed());
        assertTrue(decision.retryAfterSeconds() >= 1);
    }

    @Test
    void getType_returnsTokenBucket() {
        assertEquals(RateLimitStrategyType.TOKEN_BUCKET, limiter.getType());
    }
}
