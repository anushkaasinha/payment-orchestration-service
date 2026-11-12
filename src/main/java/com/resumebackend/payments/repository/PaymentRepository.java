package com.resumebackend.payments.repository;

import com.resumebackend.payments.domain.Payment;
import com.resumebackend.payments.domain.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);
    List<Payment> findByMerchantId(Long merchantId);
    List<Payment> findByMerchantIdOrderByIdDesc(Long merchantId, Pageable pageable);

    @Query("select p.id from Payment p where p.status = :status and p.nextRetryAt <= :now")
    List<Long> findDueIds(@Param("status") PaymentStatus status, @Param("now") Instant now);
}
