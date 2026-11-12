package com.resumebackend.payments.controller;

import com.resumebackend.payments.controller.Dtos.*;
import com.resumebackend.payments.security.MerchantPrincipal;
import com.resumebackend.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant/payments")
public class MerchantPaymentController {
    private final PaymentService service;

    public MerchantPaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PaymentView> create(@AuthenticationPrincipal MerchantPrincipal me, @Valid @RequestBody CreatePaymentRequest req) {
        PaymentService.CreateResult r = service.create(me.id(), req);
        ResponseEntity.BodyBuilder b = ResponseEntity.status(r.replayed() ? HttpStatus.OK : HttpStatus.CREATED);
        if (r.replayed()) b.header("Idempotent-Replayed", "true");
        return b.body(r.payment());
    }

    @GetMapping
    public List<PaymentView> list(@AuthenticationPrincipal MerchantPrincipal me) { return service.list(me.id()); }

    @GetMapping("/{id}")
    public PaymentView get(@AuthenticationPrincipal MerchantPrincipal me, @PathVariable Long id) { return service.get(me.id(), id); }

    @PostMapping("/{id}/force-retry")
    public PaymentView forceRetry(@AuthenticationPrincipal MerchantPrincipal me, @PathVariable Long id) {
        return service.forceRetry(me.id(), id);
    }
}
