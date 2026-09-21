package com.gateway.platform.service;

import com.gateway.platform.dto.response.BillingResponse;
import com.gateway.platform.entity.BillingRecord;
import com.gateway.platform.entity.Quota;
import com.gateway.platform.entity.Subscription;
import com.gateway.platform.entity.SubscriptionPlan;
import com.gateway.platform.repository.BillingRecordRepository;
import com.gateway.platform.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final SubscriptionService subscriptionService;
    private final QuotaRepository quotaRepository;
    private final BillingRecordRepository billingRecordRepository;

    /**
     * Computes the current period's bill on the fly from real quota usage.
     * This is what the consumer dashboard's Billing page calls — it does not
     * require Stripe to be configured, since the numbers come from our own
     * usage/quota records, not from Stripe.
     */
    public BillingResponse currentEstimate(String userId) {
        Subscription subscription = subscriptionService.getActiveSubscription(userId);
        SubscriptionPlan plan = subscription.getPlan();
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);

        Quota quota = quotaRepository.findByUserIdAndPeriodStart(userId, periodStart)
                .orElse(Quota.builder().monthlyUsed(0).build());

        long actual = quota.getMonthlyUsed();
        long included = plan.getMonthlyRequestQuota();
        long overageRequests = Math.max(0, actual - included);

        BigDecimal overageAmount = plan.getOveragePricePerRequest()
                .multiply(BigDecimal.valueOf(overageRequests))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = plan.getMonthlyPrice().add(overageAmount);

        return new BillingResponse(
                plan.getName(), plan.getMonthlyPrice(), included, actual, overageRequests,
                plan.getOveragePricePerRequest(), overageAmount, total,
                periodStart, periodEnd, subscription.getStripeCustomerId(), subscription.getStripeSubscriptionId());
    }

    /** Finalizes and persists a billing record for a completed period (e.g. run monthly via a scheduler). */
    @Transactional
    public BillingRecord finalizePeriod(String userId, LocalDate periodStart) {
        Subscription subscription = subscriptionService.getActiveSubscription(userId);
        SubscriptionPlan plan = subscription.getPlan();
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);

        Quota quota = quotaRepository.findByUserIdAndPeriodStart(userId, periodStart)
                .orElse(Quota.builder().monthlyUsed(0).build());

        long actual = quota.getMonthlyUsed();
        long included = plan.getMonthlyRequestQuota();
        long overageRequests = Math.max(0, actual - included);
        BigDecimal overageAmount = plan.getOveragePricePerRequest()
                .multiply(BigDecimal.valueOf(overageRequests)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = plan.getMonthlyPrice().add(overageAmount);

        BillingRecord record = billingRecordRepository.findByUserIdAndPeriodStart(userId, periodStart)
                .orElse(BillingRecord.builder()
                        .user(subscription.getUser())
                        .plan(plan)
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .build());
        record.setIncludedRequests(included);
        record.setActualRequests(actual);
        record.setOverageRequests(overageRequests);
        record.setBasePrice(plan.getMonthlyPrice());
        record.setOverageAmount(overageAmount);
        record.setTotalAmount(total);
        record.setStatus(BillingRecord.BillingStatus.ESTIMATED);
        return billingRecordRepository.save(record);
    }
}
