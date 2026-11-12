package com.resumebackend.payments.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "payments.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDispatcher {
    private final WebhookService webhooks;

    public WebhookDispatcher(WebhookService webhooks) { this.webhooks = webhooks; }

    @Scheduled(fixedDelay = 2000)
    public void dispatch() {
        for (Long id : webhooks.dueIds()) {
            try { webhooks.deliver(id); }
            catch (Exception e) { log.warn("Webhook {} delivery errored: {}", id, e.getMessage()); }
        }
    }
}
