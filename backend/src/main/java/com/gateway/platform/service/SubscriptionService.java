package com.gateway.platform.service;

import com.gateway.platform.entity.Subscription;
import com.gateway.platform.entity.SubscriptionPlan;
import com.gateway.platform.entity.User;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Subscription createSubscription(User user, SubscriptionPlan plan) {
        LocalDate today = LocalDate.now();
        Instant periodStart = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant periodEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .build();
        return subscriptionRepository.save(subscription);
    }

    public Subscription getActiveSubscription(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_SUBSCRIPTION",
                        "No active subscription found for this user"));
    }

    public Subscription changePlan(String userId, SubscriptionPlan newPlan) {
        Subscription subscription = getActiveSubscription(userId);
        subscription.setPlan(newPlan);
        return subscriptionRepository.save(subscription);
    }
}
