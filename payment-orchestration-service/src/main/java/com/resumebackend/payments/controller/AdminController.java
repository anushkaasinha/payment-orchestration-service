package com.resumebackend.payments.controller;

import com.resumebackend.payments.dto.*;
import com.resumebackend.payments.service.MerchantService;
import com.resumebackend.payments.service.ReconciliationService;
import com.resumebackend.payments.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MerchantService merchantService;
    private final ReconciliationService reconciliationService;
    private final WebhookService webhookService;

    @PostMapping("/merchants")
    public ResponseEntity<MerchantResponse> createMerchant(@RequestBody @Valid CreateMerchantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantService.create(request));
    }

    @GetMapping("/reconciliation/{merchantId}")
    public ResponseEntity<ReconciliationResponse> reconcile(@PathVariable Long merchantId) {
        return ResponseEntity.ok(reconciliationService.reconcileMerchant(merchantId));
    }

    @GetMapping("/webhooks/dead-letter")
    public ResponseEntity<List<WebhookEventResponse>> deadLetter() {
        return ResponseEntity.ok(webhookService.deadLetterEvents());
    }
}
