package com.resumebackend.payments.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentIntentRequest(
        @NotBlank String orderId,
        @NotNull @DecimalMin(value = "1.00") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank String idempotencyKey,
        @NotBlank String webhookUrl,
        @Min(0) @Max(6) Integer maxRetries
) {
}
