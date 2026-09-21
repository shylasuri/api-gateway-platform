package com.gateway.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores only a SHA-256 hash + short prefix of the actual key.
 * The raw key is shown to the user exactly once, at creation/rotation time,
 * and is never persisted or logged in plaintext.
 */
@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_key_hash", columnList = "key_hash", unique = true),
        @Index(name = "idx_api_keys_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    /** First 8 chars of the raw key, safe to display for identification, e.g. "gw_live_ab12cd34...". */
    @Column(name = "key_prefix", nullable = false, length = 24)
    private String keyPrefix;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
