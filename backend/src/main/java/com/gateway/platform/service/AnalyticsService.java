package com.gateway.platform.service;

import com.gateway.platform.dto.response.AnalyticsResponse;
import com.gateway.platform.entity.UsageRecord;
import com.gateway.platform.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UsageRecordRepository usageRecordRepository;

    private static final Instant EPOCH_START = Instant.EPOCH;

    public AnalyticsResponse platformAnalytics(int lastNDays) {
        return build(EPOCH_START, todayStart(), monthStart(), lastNDays, null);
    }

    public AnalyticsResponse consumerAnalytics(String userId, int lastNDays) {
        return build(EPOCH_START, todayStart(), monthStart(), lastNDays, userId);
    }

    private AnalyticsResponse build(Instant allTimeFrom, Instant today, Instant monthStart, int lastNDays, String userId) {
        Instant windowStart = Instant.now().minusSeconds((long) lastNDays * 86400);

        long totalRequests;
        long requestsToday;
        long requestsThisMonth;
        long successful;
        long failed;
        long rejected;
        long rateLimitViolations;
        long quotaViolations;
        Double avgLatency;

        if (userId == null) {
            totalRequests = usageRecordRepository.countAllSince(allTimeFrom);
            requestsToday = usageRecordRepository.countAllSince(today);
            requestsThisMonth = usageRecordRepository.countAllSince(monthStart);
            successful = usageRecordRepository.countAllowedSince(allTimeFrom);
            rejected = usageRecordRepository.countRejectedSince(allTimeFrom);
            rateLimitViolations = usageRecordRepository.countByRejectionReasonSince(
                    UsageRecord.RejectionReason.RATE_LIMIT_EXCEEDED, allTimeFrom);
            quotaViolations = usageRecordRepository.countByRejectionReasonSince(
                    UsageRecord.RejectionReason.QUOTA_EXCEEDED, allTimeFrom);
            avgLatency = usageRecordRepository.averageLatencySince(allTimeFrom);
        } else {
            totalRequests = usageRecordRepository.countByUserSince(userId, allTimeFrom);
            requestsToday = usageRecordRepository.countByUserSince(userId, today);
            requestsThisMonth = usageRecordRepository.countByUserSince(userId, monthStart);
            successful = usageRecordRepository.countAllowedByUserSince(userId, allTimeFrom);
            rejected = usageRecordRepository.countRejectedByUserSince(userId, allTimeFrom);
            rateLimitViolations = usageRecordRepository.countByUserAndRejectionReasonSince(
                    userId, UsageRecord.RejectionReason.RATE_LIMIT_EXCEEDED, allTimeFrom);
            quotaViolations = usageRecordRepository.countByUserAndRejectionReasonSince(
                    userId, UsageRecord.RejectionReason.QUOTA_EXCEEDED, allTimeFrom);
            avgLatency = usageRecordRepository.averageLatencyByUserSince(userId, allTimeFrom);
        }
        failed = Math.max(0, totalRequests - successful - rejected);

        List<Map<String, Object>> requestsOverTime = toMapList(
                usageRecordRepository.requestsPerDaySince(windowStart), "date", "count");
        List<Map<String, Object>> topApis = toMapList(
                usageRecordRepository.topApisSince(windowStart), "apiId", "apiName", "count");
        List<Map<String, Object>> topConsumers = toMapList(
                usageRecordRepository.topConsumersSince(windowStart), "userId", "email", "count");

        return new AnalyticsResponse(totalRequests, requestsToday, requestsThisMonth, successful, failed,
                rejected, rateLimitViolations, quotaViolations, avgLatency, requestsOverTime, topApis, topConsumers);
    }

    private List<Map<String, Object>> toMapList(List<Object[]> rows, String... fieldNames) {
        return rows.stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < fieldNames.length && i < row.length; i++) {
                map.put(fieldNames[i], row[i]);
            }
            return map;
        }).toList();
    }

    private Instant todayStart() {
        return LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant monthStart() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
