package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.WebhookStatus;
import com.resumebackend.payments.dto.WebhookEventResponse;
import com.resumebackend.payments.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEventRepository webhookEventRepository;

    @Transactional(readOnly = true)
    public List<WebhookEventResponse> deadLetterEvents() {
        return webhookEventRepository.findTop50ByStatusOrderByUpdatedAtDesc(WebhookStatus.DEAD_LETTER)
                .stream()
                .map(event -> new WebhookEventResponse(
                        event.getId(),
                        event.getPaymentIntent().getId(),
                        event.getEventType(),
                        event.getDeliveryUrl(),
                        event.getStatus(),
                        event.getRetryCount(),
                        event.getMaxRetries(),
                        event.getLastError(),
                        event.getCreatedAt(),
                        event.getUpdatedAt()
                ))
                .toList();
    }
}
