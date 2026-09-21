package com.gateway.platform.config;

import com.gateway.platform.entity.*;
import com.gateway.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * “When my application starts, prepare some default users, plans, subscriptions,
 *  and a sample API so I can immediately test the platform.”
 * Seeds the minimum data needed to demonstrate the platform on a fresh
 * database: an ADMIN user, a sample CONSUMER user, the FREE/PRO/ENTERPRISE
 * plans (default values — editable by admins afterwards), and one sample
 * backend API wired to the sample-api Docker service. Runs only if the
 * relevant rows don't already exist, so it's safe on every restart.
 */
@Slf4j
@Component  //Create and manage an object of this class
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiRepository apiRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.consumer-email}")
    private String consumerEmail;

    @Value("${app.seed.consumer-password}")
    private String consumerPassword;

    @Value("${app.sample-api.backend-url}")
    private String sampleApiBackendUrl;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) { //first checks whether seeding is enabled.
            log.info("Seeding disabled (SEED_ENABLED=false)"); 
            return;
        }

        //Run the run() method automatically when the Spring Boot application starts.
        //3 subscription plans:

        SubscriptionPlan free = seedPlan("FREE", new BigDecimal("0.00"), 5_000, 100L, 100,
                RateLimitStrategyType.FIXED_WINDOW, null, null, null, null, new BigDecimal("0.0100"));
        SubscriptionPlan pro = seedPlan("PRO", new BigDecimal("49.00"), 100_000, null, 1000,
                RateLimitStrategyType.SLIDING_WINDOW, null, null, null, null, new BigDecimal("0.0050"));
        SubscriptionPlan enterprise = seedPlan("ENTERPRISE", new BigDecimal("499.00"), 1_000_000, null, 10000,
                RateLimitStrategyType.TOKEN_BUCKET, 10000, 200, null, null, new BigDecimal("0.0010"));

        User admin = seedUser(adminEmail, adminPassword, "Platform Admin", Role.ADMIN, null);
        User consumer = seedUser(consumerEmail, consumerPassword, "Sample Consumer", Role.CONSUMER, pro);

        seedApi();

        log.info("Seed complete. Admin login: {} / (see SEED_ADMIN_PASSWORD). Consumer login: {} / (see SEED_CONSUMER_PASSWORD).",
                adminEmail, consumerEmail);
    }

    private SubscriptionPlan seedPlan(String name, BigDecimal price, long monthlyQuota, Long dailyQuota,
                                       int rateLimit, RateLimitStrategyType strategy,
                                       Integer bucketCapacity, Integer refillRate,
                                       Integer queueCapacity, Integer processingRate,
                                       BigDecimal overagePrice) {
        return planRepository.findByName(name).orElseGet(() -> planRepository.save(
                SubscriptionPlan.builder() //First check if this plan alr exists.If it doesn't exist,create it
                        .name(name)
                        .monthlyPrice(price)
                        .monthlyRequestQuota(monthlyQuota)
                        .dailyRequestQuota(dailyQuota)
                        .rateLimit(rateLimit)
                        .rateLimitWindowSeconds(60)
                        .rateLimitStrategy(strategy)
                        .bucketCapacity(bucketCapacity)
                        .refillRate(refillRate)
                        .queueCapacity(queueCapacity)
                        .processingRate(processingRate)
                        .overagePricePerRequest(overagePrice)
                        .build()));
    }

    private User seedUser(String email, String rawPassword, String fullName, Role role, SubscriptionPlan plan) {
        User user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
                User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .fullName(fullName)
                        .company("Gateway Platform")
                        .role(role)
                        .active(true)
                        .build()));

        if (plan != null && subscriptionRepository.findByUserId(user.getId()).isEmpty()) {
            LocalDate today = LocalDate.now();
            Instant periodStart = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant periodEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .currentPeriodStart(periodStart)
                    .currentPeriodEnd(periodEnd)
                    .status(Subscription.SubscriptionStatus.ACTIVE)
                    .build());
        }
        return user;
    }

    private void seedApi() { //creates a sample backend API
        apiRepository.findByGatewayRoute("/gateway/products").orElseGet(() -> apiRepository.save(
                Api.builder()
                        .name("Products API")
                        .description("Sample backend API used to demonstrate gateway routing, rate limiting and quotas.")
                        .gatewayRoute("/gateway/products")
                        .backendUrl(sampleApiBackendUrl)
                        .httpMethod(null) // any method
                        .active(true)
                        .build()));
    }
}
