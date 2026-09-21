package com.gateway.platform.repository;

import com.gateway.platform.entity.RateLimitConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RateLimitConfigurationRepository extends JpaRepository<RateLimitConfiguration, String> {
    Optional<RateLimitConfiguration> findByApiId(String apiId);
}
