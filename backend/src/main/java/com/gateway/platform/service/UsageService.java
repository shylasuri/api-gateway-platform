package com.gateway.platform.service;

import com.gateway.platform.entity.*;
import com.gateway.platform.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageRecordRepository usageRecordRepository;

    @Transactional
    public void record(User user, ApiKey apiKey, Api api, String endpoint, String method,
                        boolean allowed, UsageRecord.RejectionReason reason,
                        RateLimitStrategyType strategy, Integer responseStatus,
                        Long responseTimeMs, int requestCost) {
        UsageRecord record = UsageRecord.builder()
                .user(user)
                .apiKey(apiKey)
                .api(api)
                .endpoint(endpoint)
                .httpMethod(method)
                .requestedAt(Instant.now())
                .allowed(allowed)
                .rejectionReason(reason == null ? UsageRecord.RejectionReason.NONE : reason)
                .rateLimitStrategy(strategy)
                .responseStatus(responseStatus)
                .responseTimeMs(responseTimeMs)
                .requestCost(requestCost)
                .build();
        usageRecordRepository.save(record);
    }

    public List<UsageRecord> recentForUser(String userId) {
        return usageRecordRepository.findTop50ByUserIdOrderByRequestedAtDesc(userId);
    }
}
