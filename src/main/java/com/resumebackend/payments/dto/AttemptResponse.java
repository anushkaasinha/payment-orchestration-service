package com.resumebackend.payments.dto;

import com.resumebackend.payments.domain.AttemptStatus;

import java.time.Instant;

public record AttemptResponse(
        Long id,
        String gateway,
        String gatewayReference,
        AttemptStatus status,
        String errorCode,
        String errorMessage,
        Integer latencyMs,
        Instant attemptedAt
) {
}
