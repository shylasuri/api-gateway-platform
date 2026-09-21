package com.gateway.platform.repository;

import com.gateway.platform.entity.Api;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiRepository extends JpaRepository<Api, String> {
    Optional<Api> findByGatewayRoute(String gatewayRoute);
}
