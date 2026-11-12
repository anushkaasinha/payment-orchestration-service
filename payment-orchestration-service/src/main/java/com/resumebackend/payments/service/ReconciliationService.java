package com.resumebackend.payments.service;

import com.resumebackend.payments.dto.ReconciliationResponse;
import com.resumebackend.payments.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final MerchantService merchantService;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public ReconciliationResponse reconcileMerchant(Long merchantId) {
        merchantService.findById(merchantId);

        BigDecimal total = ledgerEntryRepository.sumAmountByMerchant(merchantId);
        Long settled = ledgerEntryRepository.countDistinctIntentsByMerchant(merchantId);

        return new ReconciliationResponse(
                merchantId,
                settled,
                total,
                "Sum of ledger entries for this merchant (credits + debits depending on accounting direction)."
        );
    }
}
