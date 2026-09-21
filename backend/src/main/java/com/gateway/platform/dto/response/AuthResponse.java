package com.gateway.platform.dto.response;

public record AuthResponse(String token, String userId, String email, String fullName, String role) {}
