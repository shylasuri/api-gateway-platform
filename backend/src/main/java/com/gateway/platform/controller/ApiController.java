package com.gateway.platform.controller;

import com.gateway.platform.dto.request.CreateApiRequest;
import com.gateway.platform.dto.request.RateLimitConfigRequest;
import com.gateway.platform.dto.request.UpdateApiRequest;
import com.gateway.platform.entity.Api;
import com.gateway.platform.entity.RateLimitConfiguration;
import com.gateway.platform.service.ApiManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "APIs")
public class ApiController {

    private final ApiManagementService apiManagementService;

    // Any authenticated user can browse registered APIs (needed for the consumer "/apis" page)
    @GetMapping("/apis")
    public ResponseEntity<List<Api>> list() {
        return ResponseEntity.ok(apiManagementService.listAll());
    }

    @GetMapping("/apis/{id}")
    public ResponseEntity<Api> get(@PathVariable String id) {
        return ResponseEntity.ok(apiManagementService.getById(id));
    }

    // Admin-only mutations (enforced by SecurityConfig's /admin/** rule + these live under /admin/apis)
    @PostMapping("/admin/apis")
    public ResponseEntity<Api> create(@Valid @RequestBody CreateApiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiManagementService.create(request));
    }

    @GetMapping("/admin/apis")
    public ResponseEntity<List<Api>> adminList() {
        return ResponseEntity.ok(apiManagementService.listAll());
    }

    @PutMapping("/admin/apis/{id}")
    public ResponseEntity<Api> update(@PathVariable String id, @RequestBody UpdateApiRequest request) {
        return ResponseEntity.ok(apiManagementService.update(id, request));
    }

    @PostMapping("/admin/apis/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        apiManagementService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/apis/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        apiManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/apis/{id}/rate-limit")
    public ResponseEntity<RateLimitConfiguration> setRateLimit(@PathVariable String id,
                                                                @Valid @RequestBody RateLimitConfigRequest request) {
        return ResponseEntity.ok(apiManagementService.setRateLimitConfig(id, request));
    }
}
