package com.gateway.platform.service.ratelimit;

import com.gateway.platform.entity.RateLimitStrategyType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the RateLimiterStrategy implementation to use at request time based
 * on RateLimitConfig.strategy(), which is always loaded from the database
 * (per-API RateLimitConfiguration, or the consumer's SubscriptionPlan). No
 * strategy is ever chosen in code — this class only wires whatever the data says.
 */
@Component
public class RateLimiterStrategyFactory {

    private final Map<RateLimitStrategyType, RateLimiterStrategy> strategies;

    public RateLimiterStrategyFactory(List<RateLimiterStrategy> allStrategies) {
        this.strategies = allStrategies.stream()
                .collect(Collectors.toMap(RateLimiterStrategy::getType, s -> s));
    }

    public RateLimiterStrategy resolve(RateLimitStrategyType type) {
        RateLimiterStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No RateLimiterStrategy registered for type: " + type);
        }
        return strategy;
    }
}
