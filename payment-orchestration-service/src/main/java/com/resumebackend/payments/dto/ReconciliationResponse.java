package com.resumebackend.payments.dto;

import java.math.BigDecimal;

public record ReconciliationResponse(
        Long merchantId,
        Long settledPayments,
        BigDecimal totalSettledAmount,
        String note
) {
}
