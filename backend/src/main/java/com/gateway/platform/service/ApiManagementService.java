package com.gateway.platform.service;

import com.gateway.platform.dto.request.CreateApiRequest;
import com.gateway.platform.dto.request.RateLimitConfigRequest;
import com.gateway.platform.dto.request.UpdateApiRequest;
import com.gateway.platform.entity.Api;
import com.gateway.platform.entity.RateLimitConfiguration;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.repository.ApiRepository;
import com.gateway.platform.repository.RateLimitConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiManagementService {

    private final ApiRepository apiRepository;
    private final RateLimitConfigurationRepository rateLimitConfigurationRepository;

    @Transactional
    public Api create(CreateApiRequest request) {
        apiRepository.findByGatewayRoute(request.gatewayRoute()).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "ROUTE_TAKEN",
                    "Gateway route '" + request.gatewayRoute() + "' is already registered");
        });

        Api api = Api.builder()
                .name(request.name())
                .description(request.description())
                .gatewayRoute(normalizeRoute(request.gatewayRoute()))
                .backendUrl(request.backendUrl())
                .httpMethod(request.httpMethod())
                .active(request.active() == null || request.active())
                .build();
        return apiRepository.save(api);
    }

    public List<Api> listAll() {
        return apiRepository.findAll();
    }

    public Api getById(String id) {
        return apiRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API_NOT_FOUND", "API not found"));
    }

    public Optional<Api> findByRoute(String route) {
        return apiRepository.findByGatewayRoute(normalizeRoute(route));
    }

    @Transactional
    public Api update(String id, UpdateApiRequest request) {
        Api api = getById(id);
        if (request.name() != null) api.setName(request.name());
        if (request.description() != null) api.setDescription(request.description());
        if (request.backendUrl() != null) api.setBackendUrl(request.backendUrl());
        if (request.httpMethod() != null) api.setHttpMethod(request.httpMethod());
        if (request.active() != null) api.setActive(request.active());
        return apiRepository.save(api);
    }

    @Transactional
    public void deactivate(String id) {
        Api api = getById(id);
        api.setActive(false);
        apiRepository.save(api);
    }

    @Transactional
    public void delete(String id) {
        apiRepository.deleteById(id);
    }

    @Transactional
    public RateLimitConfiguration setRateLimitConfig(String apiId, RateLimitConfigRequest request) {
        Api api = getById(apiId);
        RateLimitConfiguration config = rateLimitConfigurationRepository.findByApiId(apiId)
                .orElse(RateLimitConfiguration.builder().api(api).build());

        config.setStrategy(request.strategy());
        config.setLimitCount(request.limitCount());
        config.setWindowSeconds(request.windowSeconds());
        config.setBucketCapacity(request.bucketCapacity());
        config.setRefillRate(request.refillRate());
        config.setQueueCapacity(request.queueCapacity());
        config.setProcessingRate(request.processingRate());

        return rateLimitConfigurationRepository.save(config);
    }

    public Optional<RateLimitConfiguration> getRateLimitConfig(String apiId) {
        return rateLimitConfigurationRepository.findByApiId(apiId);
    }

    private String normalizeRoute(String route) {
        String trimmed = route.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
