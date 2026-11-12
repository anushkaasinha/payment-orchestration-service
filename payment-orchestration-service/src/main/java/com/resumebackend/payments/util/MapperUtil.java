package com.resumebackend.payments.util;

import com.resumebackend.payments.domain.PaymentAttempt;
import com.resumebackend.payments.domain.PaymentIntent;
import com.resumebackend.payments.dto.AttemptResponse;
import com.resumebackend.payments.dto.PaymentIntentResponse;

public final class MapperUtil {

    private MapperUtil() {
    }

    public static PaymentIntentResponse toPaymentIntentResponse(PaymentIntent p) {
        return new PaymentIntentResponse(
                p.getId(),
                p.getOrderId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getAttemptCount(),
                p.getMaxRetries(),
                p.getIdempotencyKey(),
                p.getLastFailureReason(),
                p.getNextRetryAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public static AttemptResponse toAttemptResponse(PaymentAttempt a) {
        return new AttemptResponse(
                a.getId(),
                a.getGateway(),
                a.getGatewayReference(),
                a.getStatus(),
                a.getErrorCode(),
                a.getErrorMessage(),
                a.getLatencyMs(),
                a.getAttemptedAt()
        );
    }
}
