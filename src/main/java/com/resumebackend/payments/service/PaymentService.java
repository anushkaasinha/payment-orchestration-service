package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.*;
import com.resumebackend.payments.dto.CreatePaymentIntentRequest;
import com.resumebackend.payments.dto.PaymentDetailResponse;
import com.resumebackend.payments.exception.BadRequestException;
import com.resumebackend.payments.exception.NotFoundException;
import com.resumebackend.payments.repository.*;
import com.resumebackend.payments.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MerchantService merchantService;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final GatewaySimulator gatewaySimulator;

    @Transactional
    public PaymentDetailResponse createAndProcess(Long merchantId, CreatePaymentIntentRequest request) {
        Merchant merchant = merchantService.findById(merchantId);

        paymentIntentRepository.findByMerchantIdAndOrderId(merchantId, request.orderId())
                .ifPresent(existing -> {
                    if (!existing.getIdempotencyKey().equals(request.idempotencyKey())) {
                        throw new BadRequestException("Order already exists with a different idempotency key");
                    }
                });

        PaymentIntent paymentIntent = paymentIntentRepository
                .findByMerchantIdAndIdempotencyKey(merchantId, request.idempotencyKey())
                .orElseGet(() -> paymentIntentRepository.save(PaymentIntent.builder()
                        .merchant(merchant)
                        .orderId(request.orderId().trim())
                        .amount(request.amount())
                        .currency(request.currency().trim().toUpperCase(Locale.ROOT))
                        .status(PaymentStatus.CREATED)
                        .attemptCount(0)
                        .maxRetries(request.maxRetries() == null ? 3 : request.maxRetries())
                        .idempotencyKey(request.idempotencyKey().trim())
                        .build()));

        if (paymentIntent.getStatus() == PaymentStatus.CREATED || paymentIntent.getStatus() == PaymentStatus.RETRY_SCHEDULED) {
            attemptPayment(paymentIntent, request.webhookUrl().trim());
        }

        return getById(merchantId, paymentIntent.getId());
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse getById(Long merchantId, Long paymentId) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentId)
                .filter(p -> p.getMerchant().getId().equals(merchantId))
                .orElseThrow(() -> new NotFoundException("Payment intent not found"));

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIntentIdOrderByAttemptedAtDesc(paymentId);

        return new PaymentDetailResponse(
                MapperUtil.toPaymentIntentResponse(paymentIntent),
                attempts.stream().map(MapperUtil::toAttemptResponse).collect(Collectors.toList())
        );
    }

    @Transactional
    public void retryDuePayments() {
        List<PaymentIntent> due = paymentIntentRepository.findTop50ByStatusInAndNextRetryAtBefore(
                List.of(PaymentStatus.RETRY_SCHEDULED), Instant.now());

        for (PaymentIntent paymentIntent : due) {
            String fallbackWebhookUrl = webhookEventRepository.findTop5ByPaymentIntentIdOrderByCreatedAtAsc(paymentIntent.getId()).stream()
                    .map(WebhookEvent::getDeliveryUrl)
                    .findFirst()
                    .orElse("https://merchant.example/webhook/payment");
            attemptPayment(paymentIntent, fallbackWebhookUrl);
        }
    }

    @Transactional
    public PaymentDetailResponse forceRetry(Long merchantId, Long paymentId) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentId)
                .filter(p -> p.getMerchant().getId().equals(merchantId))
                .orElseThrow(() -> new NotFoundException("Payment intent not found"));

        if (paymentIntent.getStatus() == PaymentStatus.SUCCEEDED) {
            throw new BadRequestException("Succeeded payment cannot be retried");
        }

        String fallbackWebhookUrl = webhookEventRepository.findTop5ByPaymentIntentIdOrderByCreatedAtAsc(paymentIntent.getId()).stream()
                .map(WebhookEvent::getDeliveryUrl)
                .findFirst()
                .orElse("https://merchant.example/webhook/payment");

        attemptPayment(paymentIntent, fallbackWebhookUrl);
        return getById(merchantId, paymentId);
    }

    private void attemptPayment(PaymentIntent paymentIntent, String webhookUrl) {
        paymentIntent.setStatus(PaymentStatus.PROCESSING);
        paymentIntent.setAttemptCount(paymentIntent.getAttemptCount() + 1);

        GatewayResponse gateway = gatewaySimulator.authorizeAndCapture(paymentIntent.getOrderId());

        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentIntent(paymentIntent)
                .gateway("mock-gateway")
                .gatewayReference("gw_" + Instant.now().toEpochMilli())
                .status(resolveAttemptStatus(gateway))
                .errorCode(gateway.success() ? null : gateway.code())
                .errorMessage(gateway.success() ? null : gateway.message())
                .latencyMs(gateway.latencyMs())
                .attemptedAt(Instant.now())
                .build();
        paymentAttemptRepository.save(attempt);

        if (gateway.success()) {
            paymentIntent.setStatus(PaymentStatus.SUCCEEDED);
            paymentIntent.setNextRetryAt(null);
            paymentIntent.setLastFailureReason(null);

            ledgerEntryRepository.save(LedgerEntry.builder()
                    .paymentIntent(paymentIntent)
                    .type(LedgerEntryType.DEBIT)
                    .amount(paymentIntent.getAmount())
                    .description("Debit customer wallet")
                    .createdAt(Instant.now())
                    .build());

            ledgerEntryRepository.save(LedgerEntry.builder()
                    .paymentIntent(paymentIntent)
                    .type(LedgerEntryType.CREDIT)
                    .amount(paymentIntent.getAmount())
                    .description("Credit merchant settlement")
                    .createdAt(Instant.now())
                    .build());

            webhookEventRepository.save(WebhookEvent.builder()
                    .paymentIntent(paymentIntent)
                    .eventType("PAYMENT_SUCCEEDED")
                    .deliveryUrl(webhookUrl)
                    .payload(buildPayload(paymentIntent))
                    .status(WebhookStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(5)
                    .nextRetryAt(Instant.now())
                    .build());

            paymentIntentRepository.save(paymentIntent);
            return;
        }

        paymentIntent.setLastFailureReason(gateway.code() + ": " + gateway.message());

        boolean retriesAvailable = paymentIntent.getAttemptCount() <= paymentIntent.getMaxRetries();
        if (gateway.transientFailure() && retriesAvailable) {
            long backoffSeconds = Math.min((long) Math.pow(2, paymentIntent.getAttemptCount()), 60);
            paymentIntent.setStatus(PaymentStatus.RETRY_SCHEDULED);
            paymentIntent.setNextRetryAt(Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS));
        } else {
            paymentIntent.setStatus(gateway.transientFailure() ? PaymentStatus.EXHAUSTED : PaymentStatus.FAILED);
            paymentIntent.setNextRetryAt(null);

            webhookEventRepository.save(WebhookEvent.builder()
                    .paymentIntent(paymentIntent)
                    .eventType("PAYMENT_FAILED")
                    .deliveryUrl(webhookUrl)
                    .payload(buildPayload(paymentIntent))
                    .status(WebhookStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(5)
                    .nextRetryAt(Instant.now())
                    .build());
        }

        paymentIntentRepository.save(paymentIntent);
    }

    private String buildPayload(PaymentIntent paymentIntent) {
        return "{\"paymentId\":" + paymentIntent.getId() +
                ",\"orderId\":\"" + paymentIntent.getOrderId() + "\"" +
                ",\"status\":\"" + paymentIntent.getStatus().name() + "\"" +
                ",\"amount\":" + paymentIntent.getAmount() +
                ",\"currency\":\"" + paymentIntent.getCurrency() + "\"}";
    }

    private AttemptStatus resolveAttemptStatus(GatewayResponse response) {
        if (response.success()) {
            return AttemptStatus.SUCCESS;
        }
        return response.transientFailure() ? AttemptStatus.SOFT_FAILURE : AttemptStatus.HARD_FAILURE;
    }
}
