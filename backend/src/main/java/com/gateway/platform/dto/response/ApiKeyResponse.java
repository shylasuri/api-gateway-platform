package com.gateway.platform.dto.response;

import java.time.Instant;

public record ApiKeyResponse(
        String id,
        String label,
        String keyPrefix,
        boolean revoked,
        Instant createdAt,
        Instant lastUsedAt,
        String rawKey // only populated on create/rotate, never on list
) {}
