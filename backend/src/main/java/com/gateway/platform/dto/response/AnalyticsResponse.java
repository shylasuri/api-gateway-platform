package com.gateway.platform.dto.response;

import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        long totalRequests,
        long requestsToday,
        long requestsThisMonth,
        long successfulRequests,
        long failedRequests,
        long rejectedRequests,
        long rateLimitViolations,
        long quotaViolations,
        Double avgLatencyMs,
        List<Map<String, Object>> requestsOverTime,
        List<Map<String, Object>> topApis,
        List<Map<String, Object>> topConsumers
) {}
