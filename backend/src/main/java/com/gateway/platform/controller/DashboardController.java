package com.gateway.platform.controller;

import com.gateway.platform.dto.response.AnalyticsResponse;
import com.gateway.platform.dto.response.BillingResponse;
import com.gateway.platform.dto.response.DashboardSummaryResponse;
import com.gateway.platform.entity.Subscription;
import com.gateway.platform.entity.UsageRecord;
import com.gateway.platform.security.GatewayUserPrincipal;
import com.gateway.platform.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Consumer Dashboard")
public class DashboardController {

    private final SubscriptionService subscriptionService;
    private final QuotaService quotaService;
    private final AnalyticsService analyticsService;
    private final UsageService usageService;
    private final BillingService billingService;

    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal GatewayUserPrincipal principal) {
        Subscription subscription = subscriptionService.getActiveSubscription(principal.getUserId());
        var plan = subscription.getPlan();
        var quotaCheck = quotaService.check(subscription.getUser(), plan);
        AnalyticsResponse analytics = analyticsService.consumerAnalytics(principal.getUserId(), 30);

        long totalConsumerRequests = analytics.successfulRequests() + analytics.failedRequests() + analytics.rejectedRequests();
        double successRate = totalConsumerRequests == 0 ? 100.0
                : (analytics.successfulRequests() * 100.0) / totalConsumerRequests;
        double errorRate = totalConsumerRequests == 0 ? 0.0 : 100.0 - successRate;

        return new DashboardSummaryResponse(
                plan.getName(),
                analytics.requestsToday(),
                plan.getDailyRequestQuota() == null ? -1 : plan.getDailyRequestQuota(),
                analytics.requestsThisMonth(),
                plan.getMonthlyRequestQuota(),
                quotaCheck.remainingMonthly(),
                plan.getRateLimit(),
                plan.getRateLimitWindowSeconds(),
                successRate,
                errorRate,
                analytics.avgLatencyMs()
        );
    }

    @GetMapping("/usage")
    public List<UsageRecord> usage(@AuthenticationPrincipal GatewayUserPrincipal principal) {
        return usageService.recentForUser(principal.getUserId());
    }

    @GetMapping("/usage/analytics")
    public AnalyticsResponse usageAnalytics(@AuthenticationPrincipal GatewayUserPrincipal principal,
                                             @RequestParam(defaultValue = "30") int days) {
        return analyticsService.consumerAnalytics(principal.getUserId(), days);
    }

    @GetMapping("/billing")
    public BillingResponse billing(@AuthenticationPrincipal GatewayUserPrincipal principal) {
        return billingService.currentEstimate(principal.getUserId());
    }
}
