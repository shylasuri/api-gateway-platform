package com.gateway.platform.service;

import com.gateway.platform.dto.request.CreatePlanRequest;
import com.gateway.platform.entity.SubscriptionPlan;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final SubscriptionPlanRepository planRepository;

    public List<SubscriptionPlan> listAll() {
        return planRepository.findAll();
    }

    public SubscriptionPlan getById(String id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Plan not found"));
    }

    public SubscriptionPlan getByName(String name) {
        return planRepository.findByName(name)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Plan '" + name + "' not found"));
    }

    @Transactional
    public SubscriptionPlan create(CreatePlanRequest request) {
        planRepository.findByName(request.name()).ifPresent(p -> {
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_EXISTS", "A plan named '" + request.name() + "' already exists");
        });

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.name())
                .monthlyPrice(request.monthlyPrice())
                .monthlyRequestQuota(request.monthlyRequestQuota())
                .dailyRequestQuota(request.dailyRequestQuota())
                .rateLimit(request.rateLimit())
                .rateLimitWindowSeconds(request.rateLimitWindowSeconds() > 0 ? request.rateLimitWindowSeconds() : 60)
                .rateLimitStrategy(request.rateLimitStrategy())
                .bucketCapacity(request.bucketCapacity())
                .refillRate(request.refillRate())
                .queueCapacity(request.queueCapacity())
                .processingRate(request.processingRate())
                .overagePricePerRequest(request.overagePricePerRequest())
                .build();
        return planRepository.save(plan);
    }

    @Transactional
    public SubscriptionPlan update(String id, CreatePlanRequest request) {
        SubscriptionPlan plan = getById(id);
        plan.setName(request.name());
        plan.setMonthlyPrice(request.monthlyPrice());
        plan.setMonthlyRequestQuota(request.monthlyRequestQuota());
        plan.setDailyRequestQuota(request.dailyRequestQuota());
        plan.setRateLimit(request.rateLimit());
        plan.setRateLimitWindowSeconds(request.rateLimitWindowSeconds() > 0 ? request.rateLimitWindowSeconds() : 60);
        plan.setRateLimitStrategy(request.rateLimitStrategy());
        plan.setBucketCapacity(request.bucketCapacity());
        plan.setRefillRate(request.refillRate());
        plan.setQueueCapacity(request.queueCapacity());
        plan.setProcessingRate(request.processingRate());
        plan.setOveragePricePerRequest(request.overagePricePerRequest());
        return planRepository.save(plan);
    }
}
