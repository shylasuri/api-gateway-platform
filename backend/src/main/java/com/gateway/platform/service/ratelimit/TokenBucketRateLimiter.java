package com.gateway.platform.service.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Token bucket: a bucket holds up to `bucketCapacity` tokens and refills at
 * `refillRate` tokens/second. Each request consumes 1 token (request cost is
 * fixed at 1 here; extend ARGV to support variable cost if needed). Shared
 * state (current tokens + last refill timestamp) lives in a Redis hash so all
 * gateway instances see the same bucket. The whole refill-then-consume
 * operation runs as one Lua script for concurrency safety — no other request
 * can interleave between "check tokens" and "decrement tokens".
 * Key structure: "rl:token:{rateLimitKey}".
 */
@Component
public class TokenBucketRateLimiter implements RateLimiterStrategy {

    private static final String SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = 1

            local bucket = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(bucket[1])
            local last_ts = tonumber(bucket[2])

            if tokens == nil then
                tokens = capacity
                last_ts = now
            end

            local elapsed = math.max(0, now - last_ts)
            local refill = elapsed * refill_rate
            tokens = math.min(capacity, tokens + refill)

            local allowed = 0
            if tokens >= requested then
                tokens = tokens - requested
                allowed = 1
            end

            redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
            redis.call('EXPIRE', key, math.ceil(capacity / refill_rate) + 60)

            return {allowed, math.floor(tokens)}
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(SCRIPT, List.class);
    }

    @Override
    public RateLimitStrategyType getType() {
        return RateLimitStrategyType.TOKEN_BUCKET;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RateLimitDecision checkAndRecord(String rateLimitKey, RateLimitConfig config) {
        String redisKey = "rl:token:" + rateLimitKey;
        double nowSeconds = System.currentTimeMillis() / 1000.0;

        List<Long> result = redis.execute(
                script,
                List.of(redisKey),
                String.valueOf(config.bucketCapacity()),
                String.valueOf(config.refillRate()),
                String.valueOf(nowSeconds));

        boolean allowed = result.get(0) == 1;
        long tokensLeft = result.get(1);

        if (allowed) {
            return RateLimitDecision.allow(config.bucketCapacity(), tokensLeft);
        }
        long retryAfter = Math.max(1, (long) Math.ceil(1.0 / config.refillRate()));
        return RateLimitDecision.reject(config.bucketCapacity(), retryAfter);
    }
}
