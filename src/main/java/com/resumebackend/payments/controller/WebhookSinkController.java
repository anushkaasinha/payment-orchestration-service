package com.resumebackend.payments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Demo receiver so webhooks can be shown as DELIVERED. Point a payment's webhookUrl at <host>/webhook-sink. */
@RestController
public class WebhookSinkController {
    @PostMapping("/webhook-sink")
    public ResponseEntity<Void> sink() { return ResponseEntity.ok().build(); }
}
