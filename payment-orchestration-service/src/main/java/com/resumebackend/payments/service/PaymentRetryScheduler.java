package com.resumebackend.payments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${app.payment-retry-delay-ms:3000}")
    public void runRetryCycle() {
        paymentService.retryDuePayments();
    }
}
