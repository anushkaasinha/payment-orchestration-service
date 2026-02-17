package com.resumebackend.payments.domain;

public enum WebhookStatus {
    PENDING,
    DELIVERED,
    RETRYING,
    DEAD_LETTER
}
