package com.resumebackend.payments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_intents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_order", columnNames = {"merchant_id", "order_id"}),
        @UniqueConstraint(name = "uk_merchant_idempotency", columnNames = {"merchant_id", "idempotency_key"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_failure_reason")
    private String lastFailureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.attemptCount == null) {
            this.attemptCount = 0;
        }
        if (this.maxRetries == null) {
            this.maxRetries = 3;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
