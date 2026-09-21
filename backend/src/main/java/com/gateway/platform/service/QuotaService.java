package com.gateway.platform.service;

import com.gateway.platform.entity.Quota;
import com.gateway.platform.entity.SubscriptionPlan;
import com.gateway.platform.entity.User;
import com.gateway.platform.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Quota accounting is deliberately kept in Postgres (not Redis): it is the
 * durable, auditable source of truth for billing, whereas Redis is used only
 * for high-frequency rate-limit decisions in the ratelimit package.
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final QuotaRepository quotaRepository;

    public record QuotaCheckResult(boolean exceeded, long remainingMonthly, long remainingDaily) {}

    @Transactional
    public Quota getOrCreateCurrentPeriod(User user, SubscriptionPlan plan) {
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        return quotaRepository.findByUserIdAndPeriodStart(user.getId(), periodStart)
                .map(q -> refreshDailyBucketIfNeeded(q, plan))
                .orElseGet(() -> quotaRepository.save(Quota.builder()
                        .user(user)
                        .periodStart(periodStart)
                        .monthlyQuota(plan.getMonthlyRequestQuota())
                        .monthlyUsed(0)
                        .dailyQuota(plan.getDailyRequestQuota())
                        .dailyUsed(0)
                        .dailyDate(LocalDate.now())
                        .build()));
    }

    private Quota refreshDailyBucketIfNeeded(Quota quota, SubscriptionPlan plan) {
        quota.setMonthlyQuota(plan.getMonthlyRequestQuota());
        quota.setDailyQuota(plan.getDailyRequestQuota());
        if (quota.getDailyDate() == null || !quota.getDailyDate().equals(LocalDate.now())) {
            quota.setDailyDate(LocalDate.now());
            quota.setDailyUsed(0);
        }
        return quota;
    }

    /** Checks remaining quota WITHOUT consuming it — used before forwarding a request. */
    public QuotaCheckResult check(User user, SubscriptionPlan plan) {
        Quota quota = getOrCreateCurrentPeriod(user, plan);
        boolean monthlyExceeded = quota.getMonthlyUsed() >= quota.getMonthlyQuota();
        boolean dailyExceeded = quota.getDailyQuota() != null && quota.getDailyUsed() >= quota.getDailyQuota();
        return new QuotaCheckResult(monthlyExceeded || dailyExceeded, quota.remainingMonthly(),
                quota.getDailyQuota() == null ? Long.MAX_VALUE : Math.max(0, quota.getDailyQuota() - quota.getDailyUsed()));
    }

    /** Consumes 1 unit of quota after a request has been allowed and forwarded. */
    @Transactional
    public void consume(User user, SubscriptionPlan plan, int cost) {
        Quota quota = getOrCreateCurrentPeriod(user, plan);
        quota.setMonthlyUsed(quota.getMonthlyUsed() + cost);
        quota.setDailyUsed(quota.getDailyUsed() + cost);
        quotaRepository.save(quota);
    }
}
