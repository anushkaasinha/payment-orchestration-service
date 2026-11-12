package com.resumebackend.payments.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "ledger_entries") @Getter @Setter @NoArgsConstructor
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_id", nullable = false) private Long paymentId;
    @Column(name = "merchant_id", nullable = false) private Long merchantId;
    @Column(nullable = false) private String account;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LedgerType type;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    private String currency;
    private Instant createdAt = Instant.now();
}
