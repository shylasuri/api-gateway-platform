package com.gateway.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name; // FREE, PRO, ENTERPRISE (extensible, not hardcoded logic)

    @Column(name = "monthly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "monthly_request_quota", nullable = false)
    private long monthlyRequestQuota;

    @Column(name = "daily_request_quota")
    private Long dailyRequestQuota;

    @Column(name = "rate_limit", nullable = false)
    private int rateLimit;

    @Column(name = "rate_limit_window_seconds", nullable = false)
    @Builder.Default
    private int rateLimitWindowSeconds = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_limit_strategy", nullable = false, length = 30)
    private RateLimitStrategyType rateLimitStrategy;

    @Column(name = "bucket_capacity")
    private Integer bucketCapacity;

    @Column(name = "refill_rate")
    private Integer refillRate;

    @Column(name = "queue_capacity")
    private Integer queueCapacity;

    @Column(name = "processing_rate")
    private Integer processingRate;

    @Column(name = "overage_price_per_request", nullable = false, precision = 10, scale = 4)
    private BigDecimal overagePricePerRequest;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
