package com.resumebackend.payments.gateway;

import com.resumebackend.payments.domain.GatewayName;
import com.resumebackend.payments.domain.GatewayOutcome;
import java.math.BigDecimal;

/** A single payment provider. Real integrations (PayU, Paytm, Stripe SDKs) would implement this same contract. */
public interface PaymentGateway {
    GatewayName name();
    GatewayResult charge(ChargeRequest request);

    record ChargeRequest(String reference, BigDecimal amount, String currency) {}
    record GatewayResult(GatewayOutcome outcome, String reference, String code, String message, long latencyMs) {}
}
