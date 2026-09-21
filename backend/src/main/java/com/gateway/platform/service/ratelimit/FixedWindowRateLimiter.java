package com.gateway.platform.service.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fixed window counter: e.g. 100 requests / 60 seconds.
 * Requests 1-100 in the window ALLOW, request 101 REJECTs with 429 until the
 * window rolls over. Key structure: "rl:fixed:{rateLimitKey}:{windowIndex}".
 * INCR + EXPIRE are executed atomically via a Lua script so concurrent
 * requests can never both "win" the last slot (no read-then-write race).
 */
@Component
public class FixedWindowRateLimiter implements RateLimiterStrategy {

    private static final String SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return {current, ttl}
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    public FixedWindowRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(SCRIPT, List.class);
    }

    @Override
    public RateLimitStrategyType getType() {
        return RateLimitStrategyType.FIXED_WINDOW;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RateLimitDecision checkAndRecord(String rateLimitKey, RateLimitConfig config) {
        long windowIndex = System.currentTimeMillis() / 1000 / config.windowSeconds();
        String redisKey = "rl:fixed:" + rateLimitKey + ":" + windowIndex;

        List<Long> result = redis.execute(script, List.of(redisKey), String.valueOf(config.windowSeconds()));
        long current = result.get(0);
        long ttl = result.get(1);

        if (current > config.limit()) {
            long retryAfter = ttl > 0 ? ttl : config.windowSeconds();
            return RateLimitDecision.reject(config.limit(), retryAfter);
        }
        return RateLimitDecision.allow(config.limit(), config.limit() - current);
    }
}
