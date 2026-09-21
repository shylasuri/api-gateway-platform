package com.gateway.platform.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingResponse(
        String planName,
        BigDecimal monthlyPrice,
        long includedRequests,
        long actualRequests,
        long overageRequests,
        BigDecimal overagePricePerRequest,
        BigDecimal overageAmount,
        BigDecimal estimatedTotal,
        LocalDate periodStart,
        LocalDate periodEnd,
        String stripeCustomerId,
        String stripeSubscriptionId
) {}
