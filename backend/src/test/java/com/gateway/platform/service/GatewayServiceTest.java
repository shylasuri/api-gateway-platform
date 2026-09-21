package com.gateway.platform.service;

import com.gateway.platform.entity.*;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.service.ratelimit.RateLimitDecision;
import com.gateway.platform.service.ratelimit.RateLimiterStrategy;
import com.gateway.platform.service.ratelimit.RateLimiterStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GatewayServiceTest {

    @Mock private ApiKeyService apiKeyService;
    @Mock private ApiManagementService apiManagementService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private RateLimiterStrategyFactory rateLimiterStrategyFactory;
    @Mock private QuotaService quotaService;
    @Mock private UsageService usageService;
    @Mock private RestTemplate restTemplate;
    @Mock private RateLimiterStrategy rateLimiterStrategy;

    private GatewayService gatewayService;

    private User user;
    private ApiKey apiKey;
    private Api api;
    private SubscriptionPlan plan;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gatewayService = new GatewayService(apiKeyService, apiManagementService, subscriptionService,
                rateLimiterStrategyFactory, quotaService, usageService, restTemplate);

        user = User.builder().id("user-1").email("a@b.com").build();
        apiKey = ApiKey.builder().id("key-1").user(user).build();
        api = Api.builder().id("api-1").name("Products API").gatewayRoute("/gateway/products")
                .backendUrl("http://sample-api:8081/products").active(true).build();
        plan = SubscriptionPlan.builder().id("plan-1").name("PRO").monthlyPrice(BigDecimal.TEN)
                .monthlyRequestQuota(100_000).rateLimit(1000).rateLimitWindowSeconds(60)
                .rateLimitStrategy(RateLimitStrategyType.FIXED_WINDOW).build();
        subscription = Subscription.builder().user(user).plan(plan).build();
    }

    @Test
    void validRequest_isForwardedAndRecordedAsAllowed() {
        when(apiKeyService.authenticate("valid-key")).thenReturn(Optional.of(apiKey));
        when(apiManagementService.findByRoute("/gateway/products")).thenReturn(Optional.of(api));
        when(subscriptionService.getActiveSubscription("user-1")).thenReturn(subscription);
        when(apiManagementService.getRateLimitConfig("api-1")).thenReturn(Optional.empty());
        when(rateLimiterStrategyFactory.resolve(RateLimitStrategyType.FIXED_WINDOW)).thenReturn(rateLimiterStrategy);
        when(rateLimiterStrategy.checkAndRecord(any(), any())).thenReturn(RateLimitDecision.allow(1000, 999));
        when(quotaService.check(user, plan)).thenReturn(new QuotaService.QuotaCheckResult(false, 99_999, Long.MAX_VALUE));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[{\"id\":1,\"name\":\"Laptop\"}]"));

        ResponseEntity<String> response = gatewayService.handle(
                "valid-key", "/gateway/products", HttpMethod.GET, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(usageService).record(eq(user), eq(apiKey), eq(api), eq("/gateway/products"), eq("GET"),
                eq(true), isNull(), eq(RateLimitStrategyType.FIXED_WINDOW), eq(200), any(), eq(1));
        verify(quotaService).consume(user, plan, 1);
    }

    @Test
    void invalidApiKey_throwsUnauthorized_andNeverForwards() {
        when(apiKeyService.authenticate("bad-key")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> gatewayService.handle("bad-key", "/gateway/products", HttpMethod.GET, null));

        assertEquals("UNAUTHORIZED", ex.getErrorCode());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void unregisteredRoute_returns404() {
        when(apiKeyService.authenticate("valid-key")).thenReturn(Optional.of(apiKey));
        when(apiManagementService.findByRoute("/gateway/unknown")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> gatewayService.handle("valid-key", "/gateway/unknown", HttpMethod.GET, null));

        assertEquals("API_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void rateLimitExceeded_returns429AndRecordsRejection_neverForwards() {
        when(apiKeyService.authenticate("valid-key")).thenReturn(Optional.of(apiKey));
        when(apiManagementService.findByRoute("/gateway/products")).thenReturn(Optional.of(api));
        when(subscriptionService.getActiveSubscription("user-1")).thenReturn(subscription);
        when(apiManagementService.getRateLimitConfig("api-1")).thenReturn(Optional.empty());
        when(rateLimiterStrategyFactory.resolve(RateLimitStrategyType.FIXED_WINDOW)).thenReturn(rateLimiterStrategy);
        when(rateLimiterStrategy.checkAndRecord(any(), any())).thenReturn(RateLimitDecision.reject(1000, 30));

        ApiException ex = assertThrows(ApiException.class,
                () -> gatewayService.handle("valid-key", "/gateway/products", HttpMethod.GET, null));

        assertEquals("RATE_LIMIT_EXCEEDED", ex.getErrorCode());
        assertEquals(30L, ex.getRetryAfterSeconds());
        verifyNoInteractions(restTemplate);
        verify(usageService).record(eq(user), eq(apiKey), eq(api), any(), any(), eq(false),
                eq(UsageRecord.RejectionReason.RATE_LIMIT_EXCEEDED), any(), eq(429), isNull(), eq(0));
    }

    @Test
    void quotaExceeded_returns429_distinctFromRateLimit_neverForwards() {
        when(apiKeyService.authenticate("valid-key")).thenReturn(Optional.of(apiKey));
        when(apiManagementService.findByRoute("/gateway/products")).thenReturn(Optional.of(api));
        when(subscriptionService.getActiveSubscription("user-1")).thenReturn(subscription);
        when(apiManagementService.getRateLimitConfig("api-1")).thenReturn(Optional.empty());
        when(rateLimiterStrategyFactory.resolve(RateLimitStrategyType.FIXED_WINDOW)).thenReturn(rateLimiterStrategy);
        when(rateLimiterStrategy.checkAndRecord(any(), any())).thenReturn(RateLimitDecision.allow(1000, 500));
        when(quotaService.check(user, plan)).thenReturn(new QuotaService.QuotaCheckResult(true, 0, 0));

        ApiException ex = assertThrows(ApiException.class,
                () -> gatewayService.handle("valid-key", "/gateway/products", HttpMethod.GET, null));

        assertEquals("QUOTA_EXCEEDED", ex.getErrorCode());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void inactiveApi_returns503_neverForwards() {
        Api inactiveApi = Api.builder().id("api-2").gatewayRoute("/gateway/products")
                .backendUrl("http://x").active(false).build();
        when(apiKeyService.authenticate("valid-key")).thenReturn(Optional.of(apiKey));
        when(apiManagementService.findByRoute("/gateway/products")).thenReturn(Optional.of(inactiveApi));

        ApiException ex = assertThrows(ApiException.class,
                () -> gatewayService.handle("valid-key", "/gateway/products", HttpMethod.GET, null));

        assertEquals("API_INACTIVE", ex.getErrorCode());
        verifyNoInteractions(restTemplate);
    }
}
