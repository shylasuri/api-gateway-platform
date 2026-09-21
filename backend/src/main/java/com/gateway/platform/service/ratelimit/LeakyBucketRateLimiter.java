package com.gateway.platform.service.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Leaky bucket (meter variant): the bucket holds a virtual "queue level" that
 * leaks at a fixed `processingRate` (units/second) regardless of arrival rate,
 * which is exactly what smooths bursty traffic into steady output. Each
 * incoming request first lets the bucket leak based on elapsed time, then
 * tries to add 1 unit. If doing so would exceed `queueCapacity`, the queue is
 * full and the request is rejected with 429; otherwise it's queued/allowed and
 * will be drained at the configured processing rate.
 * Key structure: "rl:leaky:{rateLimitKey}".
 */
@Component
public class LeakyBucketRateLimiter implements RateLimiterStrategy {

    private static final String SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local leak_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            local bucket = redis.call('HMGET', key, 'level', 'ts')
            local level = tonumber(bucket[1])
            local last_ts = tonumber(bucket[2])

            if level == nil then
                level = 0
                last_ts = now
            end

            local elapsed = math.max(0, now - last_ts)
            local leaked = elapsed * leak_rate
            level = math.max(0, level - leaked)

            local allowed = 0
            if level + 1 <= capacity then
                level = level + 1
                allowed = 1
            end

            redis.call('HMSET', key, 'level', level, 'ts', now)
            redis.call('EXPIRE', key, math.ceil(capacity / leak_rate) + 60)

            return {allowed, math.floor(capacity - level)}
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    public LeakyBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(SCRIPT, List.class);
    }

    @Override
    public RateLimitStrategyType getType() {
        return RateLimitStrategyType.LEAKY_BUCKET;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RateLimitDecision checkAndRecord(String rateLimitKey, RateLimitConfig config) {
        String redisKey = "rl:leaky:" + rateLimitKey;
        double nowSeconds = System.currentTimeMillis() / 1000.0;

        List<Long> result = redis.execute(
                script,
                List.of(redisKey),
                String.valueOf(config.queueCapacity()),
                String.valueOf(config.processingRate()),
                String.valueOf(nowSeconds));

        boolean allowed = result.get(0) == 1;
        long spaceLeft = result.get(1);

        if (allowed) {
            return RateLimitDecision.allow(config.queueCapacity(), spaceLeft);
        }
        long retryAfter = Math.max(1, (long) Math.ceil(1.0 / config.processingRate()));
        return RateLimitDecision.reject(config.queueCapacity(), retryAfter);
    }
}
