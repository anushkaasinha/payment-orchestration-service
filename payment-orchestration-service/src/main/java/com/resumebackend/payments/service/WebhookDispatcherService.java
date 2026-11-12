package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.WebhookEvent;
import com.resumebackend.payments.domain.WebhookStatus;
import com.resumebackend.payments.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcherService {

    private final WebhookEventRepository webhookEventRepository;

    @Scheduled(fixedDelayString = "${app.webhook-dispatch-delay-ms:4000}")
    @Transactional
    public void dispatchDueEvents() {
        List<WebhookEvent> events = webhookEventRepository.findTop100ByStatusInAndNextRetryAtBefore(
                List.of(WebhookStatus.PENDING, WebhookStatus.RETRYING), Instant.now());

        for (WebhookEvent event : events) {
            boolean delivered = ThreadLocalRandom.current().nextInt(100) < 78;
            if (delivered) {
                event.setStatus(WebhookStatus.DELIVERED);
                event.setLastError(null);
                event.setNextRetryAt(null);
            } else {
                int retry = event.getRetryCount() + 1;
                event.setRetryCount(retry);
                event.setLastError("HTTP 503 from downstream webhook receiver");

                if (retry > event.getMaxRetries()) {
                    event.setStatus(WebhookStatus.DEAD_LETTER);
                    event.setNextRetryAt(null);
                    log.warn("Webhook event {} moved to dead-letter queue", event.getId());
                } else {
                    long delay = Math.min((long) Math.pow(2, retry), 120);
                    event.setStatus(WebhookStatus.RETRYING);
                    event.setNextRetryAt(Instant.now().plus(delay, ChronoUnit.SECONDS));
                }
            }
            webhookEventRepository.save(event);
        }
    }
}
