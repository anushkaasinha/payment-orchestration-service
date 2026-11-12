package com.resumebackend.payments.service;

import com.resumebackend.payments.controller.Dtos.ReconciliationView;
import com.resumebackend.payments.domain.*;
import com.resumebackend.payments.exception.ApiException;
import com.resumebackend.payments.repository.LedgerEntryRepository;
import com.resumebackend.payments.repository.MerchantRepository;
import com.resumebackend.payments.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {
    private final MerchantRepository merchants;
    private final PaymentRepository payments;
    private final LedgerEntryRepository ledger;

    public ReconciliationService(MerchantRepository merchants, PaymentRepository payments, LedgerEntryRepository ledger) {
        this.merchants = merchants;
        this.payments = payments;
        this.ledger = ledger;
    }

    public ReconciliationView reconcile(Long merchantId) {
        Merchant m = merchants.findById(merchantId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND", "No merchant " + merchantId));
        List<Payment> ps = payments.findByMerchantId(merchantId);
        List<LedgerEntry> le = ledger.findByMerchantId(merchantId);
        List<Payment> ok = ps.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).toList();

        Map<String, Long> byStatus = ps.stream().collect(Collectors.groupingBy(p -> p.getStatus().name(), TreeMap::new, Collectors.counting()));
        Map<String, Long> byGateway = ok.stream().collect(Collectors.groupingBy(p -> p.getGateway().name(), TreeMap::new, Collectors.counting()));
        BigDecimal captured = ok.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = sum(le, e -> e.getType() == LedgerType.DEBIT);
        BigDecimal credit = sum(le, e -> e.getType() == LedgerType.CREDIT);
        BigDecimal fees = sum(le, e -> LedgerService.PLATFORM.equals(e.getAccount()));
        BigDecimal net = sum(le, e -> LedgerService.MERCHANT.equals(e.getAccount()));
        Set<Long> ledgered = le.stream().map(LedgerEntry::getPaymentId).collect(Collectors.toSet());
        List<Long> missing = ok.stream().map(Payment::getId).filter(id -> !ledgered.contains(id)).toList();
        boolean balanced = debit.compareTo(credit) == 0 && debit.compareTo(captured) == 0 && missing.isEmpty();

        return new ReconciliationView(m.getId(), m.getName(), byStatus, byGateway, captured, fees, net, debit, credit,
                le.size(), missing, balanced);
    }

    private static BigDecimal sum(List<LedgerEntry> entries, Predicate<LedgerEntry> filter) {
        return entries.stream().filter(filter).map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
