package com.resumebackend.payments.dto;

import com.resumebackend.payments.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentIntentResponse(
        Long id,
        String orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Integer attemptCount,
        Integer maxRetries,
        String idempotencyKey,
        String lastFailureReason,
        Instant nextRetryAt,
        Instant createdAt,
        Instant updatedAt
) {
}
