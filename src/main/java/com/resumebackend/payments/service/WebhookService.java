package com.resumebackend.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumebackend.payments.controller.Dtos.WebhookView;
import com.resumebackend.payments.domain.*;
import com.resumebackend.payments.exception.ApiException;
import com.resumebackend.payments.repository.MerchantRepository;
import com.resumebackend.payments.repository.WebhookEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Outbound webhooks: signed (HMAC-SHA256), retried with exponential backoff, dead-lettered after max attempts. */
@Service
public class WebhookService {
    private final WebhookEventRepository events;
    private final MerchantRepository merchants;
    private final ObjectMapper mapper;
    private final int maxAttempts;
    private final long baseDelaySeconds;
    private final boolean allowPrivate;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();

    public WebhookService(WebhookEventRepository events, MerchantRepository merchants, ObjectMapper mapper,
                          @Value("${payments.webhook.max-attempts}") int maxAttempts,
                          @Value("${payments.webhook.base-delay-seconds}") long baseDelaySeconds,
                          @Value("${payments.webhook.allow-private}") boolean allowPrivate) {
        this.events = events;
        this.merchants = merchants;
        this.mapper = mapper;
        this.maxAttempts = maxAttempts;
        this.baseDelaySeconds = baseDelaySeconds;
        this.allowPrivate = allowPrivate;
    }

    public void enqueue(Payment p, String eventType) {
        if (p.getWebhookUrl() == null || p.getWebhookUrl().isBlank()) return;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", eventType);
        body.put("paymentId", p.getId());
        body.put("orderId", p.getOrderId());
        body.put("amount", p.getAmount());
        body.put("currency", p.getCurrency());
        body.put("status", p.getStatus());
        body.put("gateway", p.getGateway());
        WebhookEvent e = new WebhookEvent();
        e.setPaymentId(p.getId());
        e.setMerchantId(p.getMerchantId());
        e.setEventType(eventType);
        e.setUrl(p.getWebhookUrl());
        try { e.setPayload(mapper.writeValueAsString(body)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException(ex); }
        events.save(e);
    }

    @Transactional
    public void deliver(Long id) {
        WebhookEvent e = events.findById(id).orElse(null);
        if (e == null || e.getStatus() != WebhookStatus.PENDING) return;
        Merchant m = merchants.findById(e.getMerchantId()).orElseThrow();
        e.setAttempts(e.getAttempts() + 1);
        try {
            URI uri = URI.create(e.getUrl());
            assertSafe(uri);
            HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Event", e.getEventType())
                    .header("X-Webhook-Signature", hmac(m.getWebhookSecret(), e.getPayload()))
                    .POST(HttpRequest.BodyPublishers.ofString(e.getPayload())).build();
            HttpResponse<Void> res = http.send(req, HttpResponse.BodyHandlers.discarding());
            if (res.statusCode() / 100 == 2) {
                e.setStatus(WebhookStatus.DELIVERED);
                e.setDeliveredAt(Instant.now());
                e.setLastError(null);
            } else {
                fail(e, "HTTP " + res.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail(e, "interrupted");
        } catch (Exception ex) {
            fail(e, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
        events.save(e);
    }

    private void fail(WebhookEvent e, String error) {
        e.setLastError(error.length() > 400 ? error.substring(0, 400) : error);
        if (e.getAttempts() >= maxAttempts) {
            e.setStatus(WebhookStatus.DEAD_LETTER);
        } else {
            long delay = baseDelaySeconds * (1L << (e.getAttempts() - 1));
            e.setNextAttemptAt(Instant.now().plusSeconds(delay));
        }
    }

    /** SSRF guard: public deployments must not let merchants point webhooks at internal addresses. */
    private void assertSafe(URI uri) throws Exception {
        if (uri.getHost() == null) throw new IllegalArgumentException("invalid webhook URL");
        if (allowPrivate) return;
        for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress())
                throw new IllegalArgumentException("blocked private address");
        }
    }

    private static String hmac(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    public List<Long> dueIds() {
        return events.findByStatusAndNextAttemptAtLessThanEqual(WebhookStatus.PENDING, Instant.now(), PageRequest.of(0, 20))
                .stream().map(WebhookEvent::getId).toList();
    }

    public List<WebhookView> deadLetters() {
        return events.findByStatusOrderByIdDesc(WebhookStatus.DEAD_LETTER).stream().map(WebhookService::view).toList();
    }

    @Transactional
    public WebhookView replay(Long id) {
        WebhookEvent e = events.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "WEBHOOK_NOT_FOUND", "No webhook event " + id));
        if (e.getStatus() != WebhookStatus.DEAD_LETTER)
            throw new ApiException(HttpStatus.CONFLICT, "NOT_DEAD_LETTER", "Only dead-letter events can be replayed");
        e.setStatus(WebhookStatus.PENDING);
        e.setAttempts(0);
        e.setNextAttemptAt(Instant.now());
        return view(events.save(e));
    }

    private static WebhookView view(WebhookEvent e) {
        return new WebhookView(e.getId(), e.getPaymentId(), e.getMerchantId(), e.getEventType(), e.getUrl(),
                e.getStatus(), e.getAttempts(), e.getLastError(), e.getCreatedAt());
    }
}
