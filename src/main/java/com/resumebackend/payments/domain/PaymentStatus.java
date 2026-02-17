package com.resumebackend.payments.domain;

public enum PaymentStatus {
    CREATED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED,
    EXHAUSTED
}
