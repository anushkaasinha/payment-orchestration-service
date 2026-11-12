package com.resumebackend.payments.repository;

import com.resumebackend.payments.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByMerchantId(Long merchantId);
    boolean existsByPaymentId(Long paymentId);
}
