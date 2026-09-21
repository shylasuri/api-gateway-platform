package com.gateway.platform.service;

import com.gateway.platform.entity.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Thin wrapper around the Stripe Java SDK, TEST MODE only. When
 * STRIPE_SECRET_KEY is not set (e.g. local dev without a Stripe test account),
 * every method here becomes a safe no-op instead of throwing — billing
 * calculations still run entirely off internal usage/quota data, only the
 * optional "sync to Stripe" step is skipped. This is a deliberate fallback,
 * not fake data: BillingService's numbers are always real.
 */
@Slf4j
@Service
public class StripeService {

    private final String secretKey;

    public StripeService(@Value("${app.stripe.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
    }

    @PostConstruct
    void init() {
        if (isConfigured()) {
            Stripe.apiKey = secretKey;
            log.info("Stripe test-mode integration enabled.");
        } else {
            log.warn("STRIPE_SECRET_KEY not set — Stripe sync is disabled. " +
                    "Billing amounts are still calculated from real usage data; " +
                    "only customer/subscription sync to Stripe is skipped.");
        }
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public Optional<String> createCustomer(User user) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getFullName())
                    .putMetadata("userId", user.getId())
                    .build();
            Customer customer = Customer.create(params);
            return Optional.of(customer.getId());
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer for user {}: {}", user.getId(), e.getMessage());
            return Optional.empty();
        }
    }
}
