package com.resumebackend.payments.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "merchants") @Getter @Setter @NoArgsConstructor
public class Merchant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(name = "api_key", nullable = false, unique = true) private String apiKey;
    @Column(name = "webhook_secret", nullable = false) private String webhookSecret;
    private Instant createdAt = Instant.now();
}
