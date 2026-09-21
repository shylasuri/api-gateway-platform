package com.gateway.platform.service;

import com.gateway.platform.dto.response.BillingResponse;
import com.gateway.platform.entity.*;
import com.gateway.platform.repository.BillingRecordRepository;
import com.gateway.platform.repository.QuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BillingServiceTest {

    @Mock private SubscriptionService subscriptionService;
    @Mock private QuotaRepository quotaRepository;
    @Mock private BillingRecordRepository billingRecordRepository;
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        billingService = new BillingService(subscriptionService, quotaRepository, billingRecordRepository);
    }

    @Test
    void usageWithinIncludedQuota_hasNoOverage() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("PRO").monthlyPrice(new BigDecimal("49.00"))
                .monthlyRequestQuota(100_000)
                .overagePricePerRequest(new BigDecimal("0.0050"))
                .build();
        User user = User.builder().id("user-1").build();
        Subscription subscription = Subscription.builder().user(user).plan(plan).build();
        when(subscriptionService.getActiveSubscription("user-1")).thenReturn(subscription);

        Quota quota = Quota.builder().monthlyUsed(80_000).build();
        when(quotaRepository.findByUserIdAndPeriodStart(eq("user-1"), any())).thenReturn(Optional.of(quota));

        BillingResponse response = billingService.currentEstimate("user-1");

        assertEquals(0, response.overageRequests());
        assertEquals(new BigDecimal("0.00"), response.overageAmount());
        assertEquals(new BigDecimal("49.00"), response.estimatedTotal());
    }

    @Test
    void usageExceedingQuota_calculatesOverageCorrectly() {
        // 100,000 included, 120,000 actual => 20,000 overage * 0.01 = 200.00
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("PRO").monthlyPrice(new BigDecimal("49.00"))
                .monthlyRequestQuota(100_000)
                .overagePricePerRequest(new BigDecimal("0.01"))
                .build();
        User user = User.builder().id("user-1").build();
        Subscription subscription = Subscription.builder().user(user).plan(plan).build();
        when(subscriptionService.getActiveSubscription("user-1")).thenReturn(subscription);

        Quota quota = Quota.builder().monthlyUsed(120_000).build();
        when(quotaRepository.findByUserIdAndPeriodStart(eq("user-1"), any())).thenReturn(Optional.of(quota));

        BillingResponse response = billingService.currentEstimate("user-1");

        assertEquals(20_000, response.overageRequests());
        assertEquals(new BigDecimal("200.00"), response.overageAmount());
        assertEquals(new BigDecimal("249.00"), response.estimatedTotal());
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
