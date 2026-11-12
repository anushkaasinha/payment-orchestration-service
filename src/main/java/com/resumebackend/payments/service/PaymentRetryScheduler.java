package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.PaymentStatus;
import com.resumebackend.payments.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Slf4j
@Component
@ConditionalOnProperty(name = "payments.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentRetryScheduler {
    private final PaymentRepository payments;
    private final PaymentProcessor processor;

    public PaymentRetryScheduler(PaymentRepository payments, PaymentProcessor processor) {
        this.payments = payments;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${payments.retry.poll-ms:2000}")
    public void retryDuePayments() {
        for (Long id : payments.findDueIds(PaymentStatus.RETRY_SCHEDULED, Instant.now())) {
            try { processor.process(id); }
            catch (Exception e) { log.warn("Retry of payment {} skipped: {}", id, e.getMessage()); }
        }
    }
}
