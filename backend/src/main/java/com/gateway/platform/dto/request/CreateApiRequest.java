package com.gateway.platform.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateApiRequest(
        @NotBlank String name,
        String description,
        @NotBlank String gatewayRoute,
        @NotBlank String backendUrl,
        String httpMethod,
        Boolean active
) {}
