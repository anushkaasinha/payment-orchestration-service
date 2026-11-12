package com.resumebackend.payments.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "idempotency_key"}))
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(name = "merchant_id", nullable = false) private Long merchantId;
    @Column(name = "idempotency_key", nullable = false, length = 64) private String idempotencyKey;
    @Column(nullable = false, length = 64) private String requestFingerprint;
    @Column(nullable = false, length = 64) private String orderId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(length = 500) private String webhookUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status = PaymentStatus.CREATED;
    @Enumerated(EnumType.STRING) private GatewayName gateway;
    private String gatewayReference;
    private int retryCount;
    private int maxRetries;
    private Instant nextRetryAt;
    @Column(length = 500) private String failureReason;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate void touch() { updatedAt = Instant.now(); }
}
