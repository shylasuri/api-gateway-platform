package com.gateway.platform.service;

import com.gateway.platform.entity.*;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.service.ratelimit.RateLimitConfig;
import com.gateway.platform.service.ratelimit.RateLimitDecision;
import com.gateway.platform.service.ratelimit.RateLimiterStrategy;
import com.gateway.platform.service.ratelimit.RateLimiterStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Implements the full gateway request pipeline described in the spec:
 * API key -> consumer -> plan -> rate-limit config -> rate limiter -> quota
 * check -> forward to backend -> record usage -> return response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {

    private final ApiKeyService apiKeyService;
    private final ApiManagementService apiManagementService;
    private final SubscriptionService subscriptionService;
    private final RateLimiterStrategyFactory rateLimiterStrategyFactory;
    private final QuotaService quotaService;
    private final UsageService usageService;
    private final RestTemplate gatewayRestTemplate;

    public ResponseEntity<String> handle(String rawApiKey, String route, HttpMethod method, String body) {
        long startedAt = System.currentTimeMillis();

        // 1. Validate API key
        ApiKey apiKey = apiKeyService.authenticate(rawApiKey)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Missing or invalid X-API-Key header"));
        User user = apiKey.getUser();

        // 2. Identify the registered API for this route
        Api api = apiManagementService.findByRoute(route)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API_NOT_FOUND",
                        "No API is registered for route " + route));

        if (!api.isActive()) {
            usageService.record(user, apiKey, api, route, method.name(), false,
                    UsageRecord.RejectionReason.API_INACTIVE, null, 503, null, 0);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "API_INACTIVE", "This API is currently inactive");
        }
        if (api.getHttpMethod() != null && !api.getHttpMethod().isBlank()
                && !api.getHttpMethod().equalsIgnoreCase(method.name())) {
            throw new ApiException(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                    "This API only accepts " + api.getHttpMethod());
        }

        // 3. Identify plan
        Subscription subscription = subscriptionService.getActiveSubscription(user.getId());
        SubscriptionPlan plan = subscription.getPlan();

        // 4. Load rate-limit configuration (per-API override, else plan default)
        RateLimitConfig config = resolveRateLimitConfig(api, plan);

        // 5. Rate limiter check
        RateLimiterStrategy strategy = rateLimiterStrategyFactory.resolve(config.strategy());
        String rateLimitKey = apiKey.getId() + ":" + api.getId();
        RateLimitDecision decision = strategy.checkAndRecord(rateLimitKey, config);

        if (!decision.allowed()) {
            usageService.record(user, apiKey, api, route, method.name(), false,
                    UsageRecord.RejectionReason.RATE_LIMIT_EXCEEDED, config.strategy(), 429, null, 0);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Rate limit exceeded", decision.retryAfterSeconds());
        }

        // 6. Quota check
        QuotaService.QuotaCheckResult quotaCheck = quotaService.check(user, plan);
        if (quotaCheck.exceeded()) {
            usageService.record(user, apiKey, api, route, method.name(), false,
                    UsageRecord.RejectionReason.QUOTA_EXCEEDED, config.strategy(), 429, null, 0);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "QUOTA_EXCEEDED",
                    "Monthly or daily request quota exceeded");
        }

        // 7 & 8. Forward to backend, capture status + latency
        ResponseEntity<String> backendResponse;
        Integer status;
        try {
            backendResponse = forward(api, route, method, body);
            status = backendResponse.getStatusCode().value();
        } catch (HttpStatusCodeException e) {
            status = e.getStatusCode().value();
            long latency = System.currentTimeMillis() - startedAt;
            quotaService.consume(user, plan, 1);
            usageService.record(user, apiKey, api, route, method.name(), true, null, config.strategy(),
                    status, latency, 1);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            usageService.record(user, apiKey, api, route, method.name(), false,
                    UsageRecord.RejectionReason.BACKEND_ERROR, config.strategy(), 502, null, 0);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "BACKEND_UNAVAILABLE",
                    "The backend API did not respond: " + e.getMessage());
        }

        long latencyMs = System.currentTimeMillis() - startedAt;

        // 9. Consume quota only for successfully forwarded requests
        quotaService.consume(user, plan, 1);

        // 10. Record usage
        usageService.record(user, apiKey, api, route, method.name(), true, null,
                config.strategy(), status, latencyMs, 1);

        // 11. Return response, with rate-limit headers attached
        return ResponseEntity.status(backendResponse.getStatusCode())
                .header("X-RateLimit-Limit", String.valueOf(decision.limit()))
                .header("X-RateLimit-Remaining", String.valueOf(decision.remaining()))
                .body(backendResponse.getBody());
    }

    private RateLimitConfig resolveRateLimitConfig(Api api, SubscriptionPlan plan) {
        Optional<RateLimitConfiguration> override = apiManagementService.getRateLimitConfig(api.getId());
        if (override.isPresent()) {
            RateLimitConfiguration c = override.get();
            return RateLimitConfig.builder()
                    .strategy(c.getStrategy())
                    .limit(c.getLimitCount())
                    .windowSeconds(c.getWindowSeconds())
                    .bucketCapacity(orZero(c.getBucketCapacity()))
                    .refillRate(orZero(c.getRefillRate()))
                    .queueCapacity(orZero(c.getQueueCapacity()))
                    .processingRate(orZero(c.getProcessingRate()))
                    .build();
        }
        return RateLimitConfig.builder()
                .strategy(plan.getRateLimitStrategy())
                .limit(plan.getRateLimit())
                .windowSeconds(plan.getRateLimitWindowSeconds())
                .bucketCapacity(orZero(plan.getBucketCapacity()))
                .refillRate(orZero(plan.getRefillRate()))
                .queueCapacity(orZero(plan.getQueueCapacity()))
                .processingRate(orZero(plan.getProcessingRate()))
                .build();
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private ResponseEntity<String> forward(Api api, String route, HttpMethod method, String body) {
        String suffix = route.substring(api.getGatewayRoute().length());
        String targetUrl = api.getBackendUrl() + suffix;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, headers);
        return gatewayRestTemplate.exchange(targetUrl, method, entity, String.class);
    }
}
