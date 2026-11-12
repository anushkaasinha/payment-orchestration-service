package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.*;
import com.resumebackend.payments.exception.ApiException;
import com.resumebackend.payments.gateway.GatewayRouter;
import com.resumebackend.payments.gateway.GatewayRouter.Attempt;
import com.resumebackend.payments.gateway.GatewayRouter.RouteResult;
import com.resumebackend.payments.gateway.PaymentGateway.ChargeRequest;
import com.resumebackend.payments.repository.PaymentAttemptRepository;
import com.resumebackend.payments.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * State machine: CREATED/RETRY_SCHEDULED -> (route across gateways) ->
 *   SUCCESS | FAILED (hard decline) | RETRY_SCHEDULED (all gateways transient-failed, backoff) | EXHAUSTED.
 * Payment carries @Version, so two workers racing on the same payment cannot both win.
 */
@Service
public class PaymentProcessor {
    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final GatewayRouter router;
    private final LedgerService ledger;
    private final WebhookService webhooks;
    private final long baseDelay;
    private final long maxDelay;

    public PaymentProcessor(PaymentRepository payments, PaymentAttemptRepository attempts, GatewayRouter router,
                            LedgerService ledger, WebhookService webhooks,
                            @Value("${payments.retry.base-delay-seconds}") long baseDelay,
                            @Value("${payments.retry.max-delay-seconds}") long maxDelay) {
        this.payments = payments;
        this.attempts = attempts;
        this.router = router;
        this.ledger = ledger;
        this.webhooks = webhooks;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    @Transactional
    public void process(Long id) {
        Payment p = payments.findById(id).orElseThrow();
        if (p.getStatus() != PaymentStatus.CREATED && p.getStatus() != PaymentStatus.RETRY_SCHEDULED) return;
        run(p);
    }

    @Transactional
    public void forceRetry(Long id) {
        Payment p = payments.findById(id).orElseThrow();
        if (p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.PROCESSING)
            throw new ApiException(HttpStatus.CONFLICT, "NOT_RETRYABLE", "Payment is " + p.getStatus() + " and cannot be retried");
        p.setRetryCount(0);
        run(p);
    }

    private void run(Payment p) {
        p.setStatus(PaymentStatus.PROCESSING);
        RouteResult r = router.route(new ChargeRequest("pay_" + p.getId(), p.getAmount(), p.getCurrency()));
        recordAttempts(p, r.attempts());
        List<Attempt> list = r.attempts();
        String lastMessage = list.isEmpty() ? "No gateway available" : list.get(list.size() - 1).result().message();

        if (r.success() != null) {
            p.setStatus(PaymentStatus.SUCCESS);
            p.setGateway(r.success().gateway());
            p.setGatewayReference(r.success().result().reference());
            p.setFailureReason(null);
            p.setNextRetryAt(null);
            payments.saveAndFlush(p);
            ledger.recordCapture(p);
            webhooks.enqueue(p, "payment.succeeded");
        } else if (r.hardFailure()) {
            p.setStatus(PaymentStatus.FAILED);
            p.setFailureReason(lastMessage);
            p.setNextRetryAt(null);
            payments.saveAndFlush(p);
            webhooks.enqueue(p, "payment.failed");
        } else if (p.getRetryCount() >= p.getMaxRetries()) {
            p.setStatus(PaymentStatus.EXHAUSTED);
            p.setFailureReason("All gateways unavailable after " + (p.getRetryCount() + 1) + " rounds. " + lastMessage);
            p.setNextRetryAt(null);
            payments.saveAndFlush(p);
            webhooks.enqueue(p, "payment.failed");
        } else {
            long delayMs = backoffMillis(p.getRetryCount());
            p.setRetryCount(p.getRetryCount() + 1);
            p.setStatus(PaymentStatus.RETRY_SCHEDULED);
            p.setNextRetryAt(Instant.now().plusMillis(delayMs));
            p.setFailureReason(lastMessage);
            payments.saveAndFlush(p);
        }
    }

    /** base * 2^n, capped, plus up to 20% jitter to avoid thundering herds. */
    long backoffMillis(int retryCount) {
        long seconds = Math.min(maxDelay, baseDelay << Math.min(retryCount, 20));
        long ms = seconds * 1000;
        return ms + ThreadLocalRandom.current().nextLong(0, ms / 5 + 1);
    }

    private void recordAttempts(Payment p, List<Attempt> list) {
        int n = (int) attempts.countByPaymentId(p.getId());
        for (Attempt a : list) {
            PaymentAttempt pa = new PaymentAttempt();
            pa.setPaymentId(p.getId());
            pa.setAttemptNo(++n);
            pa.setGateway(a.gateway());
            pa.setOutcome(a.result().outcome());
            pa.setCode(a.result().code());
            pa.setMessage(a.result().message());
            pa.setLatencyMs(a.result().latencyMs());
            attempts.save(pa);
        }
    }
}
