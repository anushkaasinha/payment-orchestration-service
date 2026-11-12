package com.resumebackend.payments.repository;

import com.resumebackend.payments.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("select coalesce(sum(le.amount), 0) from LedgerEntry le where le.paymentIntent.merchant.id = :merchantId")
    BigDecimal sumAmountByMerchant(Long merchantId);

    @Query("select count(distinct le.paymentIntent.id) from LedgerEntry le where le.paymentIntent.merchant.id = :merchantId")
    Long countDistinctIntentsByMerchant(Long merchantId);

    Optional<LedgerEntry> findTopByPaymentIntentIdOrderByCreatedAtDesc(Long paymentIntentId);
}
