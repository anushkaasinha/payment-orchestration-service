package com.resumebackend.payments.service;

import com.resumebackend.payments.controller.Dtos.*;
import com.resumebackend.payments.domain.Payment;
import com.resumebackend.payments.exception.ApiException;
import com.resumebackend.payments.repository.PaymentAttemptRepository;
import com.resumebackend.payments.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/** Not @Transactional on purpose: the unique-constraint race on idempotency keys must be catchable here. */
@Service
public class PaymentService {
    public record CreateResult(PaymentView payment, boolean replayed) {}

    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final PaymentProcessor processor;

    public PaymentService(PaymentRepository payments, PaymentAttemptRepository attempts, PaymentProcessor processor) {
        this.payments = payments;
        this.attempts = attempts;
        this.processor = processor;
    }

    public CreateResult create(Long merchantId, CreatePaymentRequest req) {
        String fingerprint = fingerprint(req);
        Optional<Payment> existing = payments.findByMerchantIdAndIdempotencyKey(merchantId, req.idempotencyKey());
        if (existing.isPresent()) return replay(existing.get(), fingerprint);

        Payment p = new Payment();
        p.setMerchantId(merchantId);
        p.setIdempotencyKey(req.idempotencyKey());
        p.setRequestFingerprint(fingerprint);
        p.setOrderId(req.orderId());
        p.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
        p.setCurrency(req.currency());
        p.setWebhookUrl(req.webhookUrl());
        p.setMaxRetries(req.maxRetries() == null ? 3 : req.maxRetries());
        try {
            p = payments.saveAndFlush(p);
        } catch (DataIntegrityViolationException race) {
            // A concurrent request with the same idempotency key won the insert: return its payment instead.
            Payment winner = payments.findByMerchantIdAndIdempotencyKey(merchantId, req.idempotencyKey()).orElseThrow(() -> race);
            return replay(winner, fingerprint);
        }
        processor.process(p.getId());
        return new CreateResult(view(payments.findById(p.getId()).orElseThrow()), false);
    }

    private CreateResult replay(Payment existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint))
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                    "This idempotency key was already used with a different request payload");
        return new CreateResult(view(existing), true);
    }

    public PaymentView get(Long merchantId, Long id) { return view(owned(merchantId, id)); }

    public List<PaymentView> list(Long merchantId) {
        return payments.findByMerchantIdOrderByIdDesc(merchantId, PageRequest.of(0, 20)).stream().map(this::view).toList();
    }

    public PaymentView forceRetry(Long merchantId, Long id) {
        owned(merchantId, id);
        processor.forceRetry(id);
        return view(payments.findById(id).orElseThrow());
    }

    private Payment owned(Long merchantId, Long id) {
        return payments.findById(id).filter(p -> p.getMerchantId().equals(merchantId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "No payment " + id));
    }

    private PaymentView view(Payment p) {
        List<AttemptView> a = attempts.findByPaymentIdOrderByIdAsc(p.getId()).stream()
                .map(x -> new AttemptView(x.getAttemptNo(), x.getGateway(), x.getOutcome(), x.getCode(), x.getMessage(),
                        x.getLatencyMs(), x.getCreatedAt())).toList();
        return new PaymentView(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getGateway(),
                p.getGatewayReference(), p.getRetryCount(), p.getMaxRetries(), p.getNextRetryAt(), p.getFailureReason(),
                p.getCreatedAt(), a);
    }

    private static String fingerprint(CreatePaymentRequest r) {
        try {
            String raw = r.orderId() + "|" + r.amount().setScale(2, RoundingMode.HALF_UP) + "|" + r.currency() + "|" + r.webhookUrl();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
