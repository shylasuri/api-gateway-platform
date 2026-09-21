package com.gateway.platform.service;

import com.gateway.platform.dto.response.ApiKeyResponse;
import com.gateway.platform.entity.ApiKey;
import com.gateway.platform.entity.User;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.repository.ApiKeyRepository;
import com.gateway.platform.repository.UserRepository;
import com.gateway.platform.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final ApiKeyGenerator keyGenerator;

    @Transactional
    public ApiKeyResponse create(String userId, String label) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String rawKey = keyGenerator.generateRawKey();
        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .keyHash(keyGenerator.hash(rawKey))
                .keyPrefix(keyGenerator.displayPrefix(rawKey))
                .label(label)
                .revoked(false)
                .build();
        apiKey = apiKeyRepository.save(apiKey);

        return toResponse(apiKey, rawKey);
    }

    public List<ApiKeyResponse> listForUser(String userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(k -> toResponse(k, null))
                .toList();
    }

    @Transactional
    public void revoke(String userId, String apiKeyId) {
        ApiKey apiKey = getOwned(userId, apiKeyId);
        apiKey.setRevoked(true);
        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);
    }

    @Transactional
    public ApiKeyResponse rotate(String userId, String apiKeyId) {
        ApiKey oldKey = getOwned(userId, apiKeyId);
        oldKey.setRevoked(true);
        oldKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(oldKey);

        return create(userId, oldKey.getLabel() + " (rotated)");
    }

    /** Called on every gateway request; validates the key and updates lastUsedAt. */
    @Transactional
    public Optional<ApiKey> authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String hash = keyGenerator.hash(rawKey);
        Optional<ApiKey> found = apiKeyRepository.findByKeyHash(hash);
        if (found.isEmpty() || found.get().isRevoked()) {
            return Optional.empty();
        }
        ApiKey apiKey = found.get();
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);
        return Optional.of(apiKey);
    }

    private ApiKey getOwned(String userId, String apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "API_KEY_NOT_FOUND", "API key not found"));
        if (!apiKey.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "This API key does not belong to you");
        }
        return apiKey;
    }

    private ApiKeyResponse toResponse(ApiKey key, String rawKey) {
        return new ApiKeyResponse(
                key.getId(), key.getLabel(), key.getKeyPrefix(), key.isRevoked(),
                key.getCreatedAt(), key.getLastUsedAt(), rawKey);
    }
}
