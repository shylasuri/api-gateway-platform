package com.gateway.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One row per (user, period). Period is a calendar month, identified by periodStart.
 * Kept in Postgres as the durable source of truth; Redis is only used for fast
 * rate-limiting decisions, not quota accounting.
 */
@Entity
@Table(name = "quotas", uniqueConstraints = {
        @UniqueConstraint(name = "uq_quota_user_period", columnNames = {"user_id", "period_start"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart; // first day of month

    @Column(name = "monthly_quota", nullable = false)
    private long monthlyQuota;

    @Column(name = "monthly_used", nullable = false)
    @Builder.Default
    private long monthlyUsed = 0;

    @Column(name = "daily_quota")
    private Long dailyQuota;

    @Column(name = "daily_used", nullable = false)
    @Builder.Default
    private long dailyUsed = 0;

    @Column(name = "daily_date")
    private LocalDate dailyDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public long remainingMonthly() {
        return Math.max(0, monthlyQuota - monthlyUsed);
    }
}
