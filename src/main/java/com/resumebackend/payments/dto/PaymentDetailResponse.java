package com.resumebackend.payments.dto;

import java.util.List;

public record PaymentDetailResponse(
        PaymentIntentResponse payment,
        List<AttemptResponse> attempts
) {
}
