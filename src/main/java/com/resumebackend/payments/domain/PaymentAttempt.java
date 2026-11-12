package com.resumebackend.payments.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "payment_attempts") @Getter @Setter @NoArgsConstructor
public class PaymentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_id", nullable = false) private Long paymentId;
    private int attemptNo;
    @Enumerated(EnumType.STRING) private GatewayName gateway;
    @Enumerated(EnumType.STRING) private GatewayOutcome outcome;
    private String code;
    @Column(length = 500) private String message;
    private long latencyMs;
    private Instant createdAt = Instant.now();
}
