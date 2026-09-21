package com.gateway.platform.dto.request;

public record UpdateApiRequest(
        String name,
        String description,
        String backendUrl,
        String httpMethod,
        Boolean active
) {}
