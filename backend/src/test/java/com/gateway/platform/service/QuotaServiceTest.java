package com.gateway.platform.service;

import com.gateway.platform.entity.Quota;
import com.gateway.platform.entity.RateLimitStrategyType;
import com.gateway.platform.entity.SubscriptionPlan;
import com.gateway.platform.entity.User;
import com.gateway.platform.repository.QuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuotaServiceTest {

    @Mock private QuotaRepository quotaRepository;
    private QuotaService quotaService;

    private User user;
    private SubscriptionPlan plan;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        quotaService = new QuotaService(quotaRepository);
        user = User.builder().id("user-1").build();
        plan = SubscriptionPlan.builder()
                .id("plan-1").name("PRO").monthlyPrice(BigDecimal.TEN)
                .monthlyRequestQuota(100_000).dailyRequestQuota(null)
                .rateLimit(1000).rateLimitStrategy(RateLimitStrategyType.SLIDING_WINDOW)
                .overagePricePerRequest(new BigDecimal("0.005"))
                .build();
    }

    @Test
    void usageBelowQuota_isNotExceeded() {
        Quota quota = Quota.builder().user(user).periodStart(LocalDate.now().withDayOfMonth(1))
                .monthlyQuota(100_000).monthlyUsed(82_000).dailyUsed(0).dailyDate(LocalDate.now()).build();
        when(quotaRepository.findByUserIdAndPeriodStart(eq("user-1"), any())).thenReturn(Optional.of(quota));

        QuotaService.QuotaCheckResult result = quotaService.check(user, plan);

        assertFalse(result.exceeded());
        assertEquals(18_000, result.remainingMonthly());
    }

    @Test
    void usageAtOrAboveQuota_isExceeded() {
        Quota quota = Quota.builder().user(user).periodStart(LocalDate.now().withDayOfMonth(1))
                .monthlyQuota(100_000).monthlyUsed(100_000).dailyUsed(0).dailyDate(LocalDate.now()).build();
        when(quotaRepository.findByUserIdAndPeriodStart(eq("user-1"), any())).thenReturn(Optional.of(quota));

        QuotaService.QuotaCheckResult result = quotaService.check(user, plan);

        assertTrue(result.exceeded());
        assertEquals(0, result.remainingMonthly());
    }

    @Test
    void consume_incrementsMonthlyAndDailyUsage() {
        Quota quota = Quota.builder().user(user).periodStart(LocalDate.now().withDayOfMonth(1))
                .monthlyQuota(100_000).monthlyUsed(10).dailyUsed(3).dailyDate(LocalDate.now()).build();
        when(quotaRepository.findByUserIdAndPeriodStart(eq("user-1"), any())).thenReturn(Optional.of(quota));
        when(quotaRepository.save(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

        quotaService.consume(user, plan, 1);

        assertEquals(11, quota.getMonthlyUsed());
        assertEquals(4, quota.getDailyUsed());
    }

    @Test
    void noExistingQuotaRow_createsOneFromPlanDefaults() {
        when(quotaRepository.findByUserIdAndPeriodStart(eq("user-1"), any())).thenReturn(Optional.empty());
        when(quotaRepository.save(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

        QuotaService.QuotaCheckResult result = quotaService.check(user, plan);

        assertFalse(result.exceeded());
        assertEquals(100_000, result.remainingMonthly());
    }
}
