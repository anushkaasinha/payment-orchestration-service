package com.resumebackend.payments.controller;

import com.resumebackend.payments.dto.CreatePaymentIntentRequest;
import com.resumebackend.payments.dto.PaymentDetailResponse;
import com.resumebackend.payments.service.CurrentAuthService;
import com.resumebackend.payments.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant/payments")
@RequiredArgsConstructor
public class MerchantPaymentController {

    private final CurrentAuthService currentAuthService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDetailResponse> create(@RequestBody @Valid CreatePaymentIntentRequest request) {
        Long merchantId = currentAuthService.merchantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createAndProcess(merchantId, request));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailResponse> getById(@PathVariable Long paymentId) {
        Long merchantId = currentAuthService.merchantId();
        return ResponseEntity.ok(paymentService.getById(merchantId, paymentId));
    }

    @PostMapping("/{paymentId}/force-retry")
    public ResponseEntity<PaymentDetailResponse> forceRetry(@PathVariable Long paymentId) {
        Long merchantId = currentAuthService.merchantId();
        return ResponseEntity.ok(paymentService.forceRetry(merchantId, paymentId));
    }
}
