package com.gateway.platform.controller;

import com.gateway.platform.dto.request.CreateApiKeyRequest;
import com.gateway.platform.dto.response.ApiKeyResponse;
import com.gateway.platform.security.GatewayUserPrincipal;
import com.gateway.platform.service.ApiKeyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKeyResponse> create(@AuthenticationPrincipal GatewayUserPrincipal principal,
                                                  @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.create(principal.getUserId(), request.label()));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> list(@AuthenticationPrincipal GatewayUserPrincipal principal) {
        return ResponseEntity.ok(apiKeyService.listForUser(principal.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal GatewayUserPrincipal principal,
                                        @PathVariable String id) {
        apiKeyService.revoke(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<ApiKeyResponse> rotate(@AuthenticationPrincipal GatewayUserPrincipal principal,
                                                  @PathVariable String id) {
        return ResponseEntity.ok(apiKeyService.rotate(principal.getUserId(), id));
    }
}
