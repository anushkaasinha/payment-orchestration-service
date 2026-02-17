package com.resumebackend.payments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_intent_id")
    private PaymentIntent paymentIntent;

    @Column(nullable = false)
    private String gateway;

    @Column(name = "gateway_reference", nullable = false)
    private String gatewayReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "latency_ms", nullable = false)
    private Integer latencyMs;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}
