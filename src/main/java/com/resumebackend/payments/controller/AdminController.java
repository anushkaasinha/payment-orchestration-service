package com.resumebackend.payments.controller;

import com.resumebackend.payments.controller.Dtos.*;
import com.resumebackend.payments.domain.GatewayName;
import com.resumebackend.payments.gateway.GatewayRegistry;
import com.resumebackend.payments.service.MerchantService;
import com.resumebackend.payments.service.ReconciliationService;
import com.resumebackend.payments.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final MerchantService merchants;
    private final ReconciliationService reconciliation;
    private final WebhookService webhooks;
    private final GatewayRegistry gateways;

    public AdminController(MerchantService merchants, ReconciliationService reconciliation,
                           WebhookService webhooks, GatewayRegistry gateways) {
        this.merchants = merchants;
        this.reconciliation = reconciliation;
        this.webhooks = webhooks;
        this.gateways = gateways;
    }

    @PostMapping("/merchants") @ResponseStatus(HttpStatus.CREATED)
    public MerchantResponse createMerchant(@Valid @RequestBody CreateMerchantRequest req) { return merchants.create(req.name()); }

    @GetMapping("/reconciliation/{merchantId}")
    public ReconciliationView reconcile(@PathVariable Long merchantId) { return reconciliation.reconcile(merchantId); }

    @GetMapping("/webhooks/dead-letter")
    public List<WebhookView> deadLetters() { return webhooks.deadLetters(); }

    @PostMapping("/webhooks/{id}/replay")
    public WebhookView replay(@PathVariable Long id) { return webhooks.replay(id); }

    @GetMapping("/gateways")
    public List<Map<String, Object>> gateways() { return gateways.snapshot(); }

    @PutMapping("/gateways/{name}")
    public List<Map<String, Object>> setMode(@PathVariable GatewayName name, @Valid @RequestBody GatewayModeRequest req) {
        gateways.setMode(name, req.mode());
        return gateways.snapshot();
    }
}
