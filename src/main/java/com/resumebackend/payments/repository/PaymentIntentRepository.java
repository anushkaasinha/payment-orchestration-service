package com.resumebackend.payments.repository;

import com.resumebackend.payments.domain.PaymentIntent;
import com.resumebackend.payments.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    Optional<PaymentIntent> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);

    Optional<PaymentIntent> findByMerchantIdAndOrderId(Long merchantId, String orderId);

    List<PaymentIntent> findTop50ByStatusInAndNextRetryAtBefore(List<PaymentStatus> statuses, Instant now);
}
