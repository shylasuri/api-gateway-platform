package com.gateway.platform.repository;

import com.gateway.platform.entity.ApiKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    /**
     * Eagerly fetch the owning user: this is called on the (non-transactional)
     * gateway hot path, which then reads apiKey.getUser(). With open-in-view=false
     * a lazy user would throw LazyInitializationException, so we join-fetch it here.
     */
    @EntityGraph(attributePaths = "user")
    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(String userId);
}
