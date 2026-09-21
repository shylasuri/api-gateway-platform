package com.gateway.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A backend API registered with the gateway. Admin-managed, never hardcoded.
 */
@Entity
@Table(name = "apis", indexes = {
        @Index(name = "idx_apis_route", columnList = "gateway_route", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Api {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Path the client calls on the gateway, e.g. /gateway/products */
    @Column(name = "gateway_route", nullable = false, unique = true)
    private String gatewayRoute;

    /** Where the gateway forwards to, e.g. http://sample-api:8081/products */
    @Column(name = "backend_url", nullable = false)
    private String backendUrl;

    /** Null/blank = any method allowed */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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
