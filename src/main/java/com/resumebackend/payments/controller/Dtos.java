package com.resumebackend.payments.controller;

import com.resumebackend.payments.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    public record CreateMerchantRequest(@NotBlank @Size(max = 100) String name) {}
    public record MerchantResponse(Long id, String name, String apiKey, Instant createdAt) {}

    public record CreatePaymentRequest(
            @NotBlank @Size(max = 64) String orderId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO code") String currency,
            @NotBlank @Size(max = 64) String idempotencyKey,
            @Size(max = 500) @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL") String webhookUrl,
            @Min(0) @Max(10) Integer maxRetries) {}

    public record GatewayModeRequest(@NotNull com.resumebackend.payments.gateway.GatewayMode mode) {}

    public record AttemptView(int attemptNo, GatewayName gateway, GatewayOutcome outcome, String code,
                              String message, long latencyMs, Instant at) {}

    public record PaymentView(Long id, String orderId, BigDecimal amount, String currency, PaymentStatus status,
                              GatewayName gateway, String gatewayReference, int retryCount, int maxRetries,
                              Instant nextRetryAt, String failureReason, Instant createdAt, List<AttemptView> attempts) {}

    public record WebhookView(Long id, Long paymentId, Long merchantId, String eventType, String url,
                              WebhookStatus status, int attempts, String lastError, Instant createdAt) {}

    public record ReconciliationView(Long merchantId, String merchantName, Map<String, Long> paymentsByStatus,
                                     Map<String, Long> successByGateway, BigDecimal capturedAmount,
                                     BigDecimal platformFees, BigDecimal netSettlement, BigDecimal debitTotal,
                                     BigDecimal creditTotal, int ledgerEntries, List<Long> successWithoutLedger,
                                     boolean balanced) {}
}
