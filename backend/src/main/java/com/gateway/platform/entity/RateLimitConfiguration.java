package com.gateway.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional per-API override of the plan's rate-limit configuration.
 * When present for a given Api, the gateway uses these values instead of
 * the consumer's plan defaults for requests to that API.
 */
@Entity
@Table(name = "rate_limit_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false, unique = true)
    private Api api;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 30)
    private RateLimitStrategyType strategy;

    @Column(name = "limit_count", nullable = false)
    private int limitCount;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    @Column(name = "bucket_capacity")
    private Integer bucketCapacity;

    @Column(name = "refill_rate")
    private Integer refillRate;

    @Column(name = "queue_capacity")
    private Integer queueCapacity;

    @Column(name = "processing_rate")
    private Integer processingRate;
}
