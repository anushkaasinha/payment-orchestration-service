package com.resumebackend.payments.repository;

import com.resumebackend.payments.domain.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    List<PaymentAttempt> findByPaymentIntentIdOrderByAttemptedAtDesc(Long paymentIntentId);
}
