package com.resumebackend.payments.repository;

import com.resumebackend.payments.domain.WebhookEvent;
import com.resumebackend.payments.domain.WebhookStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> findByStatusAndNextAttemptAtLessThanEqual(WebhookStatus status, Instant now, Pageable pageable);
    List<WebhookEvent> findByStatusOrderByIdDesc(WebhookStatus status);
}
