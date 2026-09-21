package com.gateway.platform.service;

import com.gateway.platform.dto.response.ApiKeyResponse;
import com.gateway.platform.entity.ApiKey;
import com.gateway.platform.entity.User;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.repository.ApiKeyRepository;
import com.gateway.platform.repository.UserRepository;
import com.gateway.platform.util.ApiKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiKeyServiceTest {

    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private UserRepository userRepository;
    private ApiKeyService apiKeyService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        apiKeyService = new ApiKeyService(apiKeyRepository, userRepository, new ApiKeyGenerator());
        user = User.builder().id("user-1").email("a@b.com").build();
    }

    @Test
    void create_generatesRawKeyOnceAndStoresOnlyHash() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            k.setId("key-1");
            return k;
        });

        ApiKeyResponse response = apiKeyService.create("user-1", "My Key");

        assertNotNull(response.rawKey());
        assertTrue(response.rawKey().startsWith("gw_live_"));
        assertNotNull(response.keyPrefix());

        // The persisted entity must never contain the raw key, only its hash
        verify(apiKeyRepository).save(argThat(k ->
                !k.getKeyHash().equals(response.rawKey()) && k.getKeyHash().length() == 64));
    }

    @Test
    void listForUser_neverExposesRawKey() {
        ApiKey stored = ApiKey.builder().id("key-1").user(user).keyHash("hash").keyPrefix("gw_live_ab...")
                .label("My Key").revoked(false).build();
        when(apiKeyRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(java.util.List.of(stored));

        var results = apiKeyService.listForUser("user-1");

        assertEquals(1, results.size());
        assertNull(results.get(0).rawKey());
    }

    @Test
    void authenticate_withRevokedKey_returnsEmpty() {
        ApiKeyGenerator generator = new ApiKeyGenerator();
        String raw = generator.generateRawKey();
        ApiKey revoked = ApiKey.builder().id("key-1").user(user).keyHash(generator.hash(raw))
                .keyPrefix("prefix").revoked(true).build();
        when(apiKeyRepository.findByKeyHash(generator.hash(raw))).thenReturn(Optional.of(revoked));

        assertTrue(apiKeyService.authenticate(raw).isEmpty());
    }

    @Test
    void authenticate_withUnknownKey_returnsEmpty() {
        when(apiKeyRepository.findByKeyHash(any())).thenReturn(Optional.empty());

        assertTrue(apiKeyService.authenticate("gw_live_doesnotexist").isEmpty());
    }

    @Test
    void revoke_ofAnotherUsersKey_throwsForbidden() {
        User otherUser = User.builder().id("user-2").build();
        ApiKey key = ApiKey.builder().id("key-1").user(otherUser).build();
        when(apiKeyRepository.findById("key-1")).thenReturn(Optional.of(key));

        ApiException ex = assertThrows(ApiException.class, () -> apiKeyService.revoke("user-1", "key-1"));
        assertEquals("ACCESS_DENIED", ex.getErrorCode());
    }
}
