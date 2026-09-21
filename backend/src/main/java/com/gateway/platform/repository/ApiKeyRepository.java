package com.gateway.platform.repository;

import com.gateway.platform.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByUserIdOrderByCreatedAtDesc(String userId);
}
