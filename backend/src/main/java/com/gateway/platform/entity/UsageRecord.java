package com.gateway.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "usage_records", indexes = {
        @Index(name = "idx_usage_user_time", columnList = "user_id, requested_at"),
        @Index(name = "idx_usage_api_time", columnList = "api_id, requested_at"),
        @Index(name = "idx_usage_requested_at", columnList = "requested_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id")
    private Api api;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(nullable = false)
    private boolean allowed;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 30)
    private RejectionReason rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_limit_strategy", length = 30)
    private RateLimitStrategyType rateLimitStrategy;

    @Column(name = "request_cost", nullable = false)
    @Builder.Default
    private int requestCost = 1;

    public enum RejectionReason {
        RATE_LIMIT_EXCEEDED, QUOTA_EXCEEDED, INVALID_API_KEY, API_INACTIVE, BACKEND_ERROR, NONE
    }
}
