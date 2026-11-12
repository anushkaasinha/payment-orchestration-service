package com.resumebackend.payments.dto;

import com.resumebackend.payments.domain.WebhookStatus;

import java.time.Instant;

public record WebhookEventResponse(
        Long id,
        Long paymentId,
        String eventType,
        String deliveryUrl,
        WebhookStatus status,
        Integer retryCount,
        Integer maxRetries,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
