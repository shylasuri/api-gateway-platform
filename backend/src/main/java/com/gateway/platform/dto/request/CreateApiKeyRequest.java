package com.gateway.platform.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequest(@NotBlank String label) {}
