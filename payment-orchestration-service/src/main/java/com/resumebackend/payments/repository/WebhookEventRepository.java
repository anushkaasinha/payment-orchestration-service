package com.resumebackend.payments.repository;

import com.resumebackend.payments.domain.WebhookEvent;
import com.resumebackend.payments.domain.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> findTop100ByStatusInAndNextRetryAtBefore(List<WebhookStatus> statuses, Instant now);

    List<WebhookEvent> findTop50ByStatusOrderByUpdatedAtDesc(WebhookStatus status);

    List<WebhookEvent> findTop5ByPaymentIntentIdOrderByCreatedAtAsc(Long paymentIntentId);
}
