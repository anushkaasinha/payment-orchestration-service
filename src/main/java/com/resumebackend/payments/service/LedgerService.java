package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.*;
import com.resumebackend.payments.repository.LedgerEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Double-entry ledger: DEBIT customer funds = CREDIT merchant payable + CREDIT platform fee. */
@Service
public class LedgerService {
    public static final String CUSTOMER = "CUSTOMER_FUNDS";
    public static final String MERCHANT = "MERCHANT_PAYABLE";
    public static final String PLATFORM = "PLATFORM_REVENUE";

    private final LedgerEntryRepository ledger;
    private final BigDecimal feePercent;

    public LedgerService(LedgerEntryRepository ledger, @Value("${payments.platform-fee-percent}") BigDecimal feePercent) {
        this.ledger = ledger;
        this.feePercent = feePercent;
    }

    public void recordCapture(Payment p) {
        if (ledger.existsByPaymentId(p.getId())) return; // idempotent
        BigDecimal fee = p.getAmount().multiply(feePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal net = p.getAmount().subtract(fee);
        ledger.save(entry(p, CUSTOMER, LedgerType.DEBIT, p.getAmount()));
        ledger.save(entry(p, MERCHANT, LedgerType.CREDIT, net));
        ledger.save(entry(p, PLATFORM, LedgerType.CREDIT, fee));
    }

    private LedgerEntry entry(Payment p, String account, LedgerType type, BigDecimal amount) {
        LedgerEntry e = new LedgerEntry();
        e.setPaymentId(p.getId());
        e.setMerchantId(p.getMerchantId());
        e.setAccount(account);
        e.setType(type);
        e.setAmount(amount);
        e.setCurrency(p.getCurrency());
        return e;
    }
}
