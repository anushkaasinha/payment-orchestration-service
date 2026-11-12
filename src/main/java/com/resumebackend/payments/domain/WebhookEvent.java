package com.resumebackend.payments.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "webhook_events") @Getter @Setter @NoArgsConstructor
public class WebhookEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_id", nullable = false) private Long paymentId;
    @Column(name = "merchant_id", nullable = false) private Long merchantId;
    @Column(nullable = false) private String eventType;
    @Column(nullable = false, length = 500) private String url;
    @Column(nullable = false, length = 4000) private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WebhookStatus status = WebhookStatus.PENDING;
    private int attempts;
    private Instant nextAttemptAt = Instant.now();
    @Column(length = 500) private String lastError;
    private Instant createdAt = Instant.now();
    private Instant deliveredAt;
}
