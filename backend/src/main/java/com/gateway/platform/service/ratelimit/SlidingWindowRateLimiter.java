package com.gateway.platform.service.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * True sliding-window log: every allowed request's timestamp (with a unique
 * member so identical millisecond timestamps don't collide) is stored in a
 * Redis sorted set scored by request time. On each check we atomically:
 *   1. Trim entries older than (now - windowSeconds)   -> handles expiry
 *   2. Count what remains                              -> handles the rolling interval, not fixed buckets
 *   3. If under the limit, add the new entry and allow  -> handles concurrency (single Lua exec = atomic)
 *   4. Otherwise reject
 * Key structure: "rl:sliding:{rateLimitKey}".
 */
@Component
public class SlidingWindowRateLimiter implements RateLimiterStrategy {

    private static final String SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window_ms = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]

            local min_score = now - window_ms
            redis.call('ZREMRANGEBYSCORE', key, '-inf', min_score)

            local count = redis.call('ZCARD', key)

            if count < limit then
                redis.call('ZADD', key, now, member)
                redis.call('PEXPIRE', key, window_ms)
                return {1, count + 1}
            else
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local retry_ms = window_ms
                if oldest[2] ~= nil then
                    retry_ms = (tonumber(oldest[2]) + window_ms) - now
                end
                return {0, count, retry_ms}
            end
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    public SlidingWindowRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(SCRIPT, List.class);
    }

    @Override
    public RateLimitStrategyType getType() {
        return RateLimitStrategyType.SLIDING_WINDOW;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RateLimitDecision checkAndRecord(String rateLimitKey, RateLimitConfig config) {
        String redisKey = "rl:sliding:" + rateLimitKey;
        long now = System.currentTimeMillis();
        long windowMs = config.windowSeconds() * 1000L;
        String member = now + ":" + UUID.randomUUID();

        List<Long> result = redis.execute(
                script,
                List.of(redisKey),
                String.valueOf(now), String.valueOf(windowMs), String.valueOf(config.limit()), member);

        boolean allowed = result.get(0) == 1;
        if (allowed) {
            long used = result.get(1);
            return RateLimitDecision.allow(config.limit(), config.limit() - used);
        } else {
            long retryAfterSeconds = Math.max(1, result.size() > 2 ? (result.get(2) / 1000) : config.windowSeconds());
            return RateLimitDecision.reject(config.limit(), retryAfterSeconds);
        }
    }
}
