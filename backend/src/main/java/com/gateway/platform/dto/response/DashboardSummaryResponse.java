package com.gateway.platform.dto.response;

public record DashboardSummaryResponse(
        String planName,
        long requestsToday,
        long dailyQuota,
        long requestsThisMonth,
        long monthlyQuota,
        long remainingQuota,
        int rateLimit,
        int rateLimitWindowSeconds,
        double successRate,
        double errorRate,
        Double avgLatencyMs
) {}
