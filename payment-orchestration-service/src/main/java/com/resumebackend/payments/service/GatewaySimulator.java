package com.resumebackend.payments.service;

import org.springframework.stereotype.Component;

@Component
public class GatewaySimulator {

    public GatewayResponse authorizeAndCapture(String orderId) {
        if (orderId != null) {
            if (orderId.contains("FORCE_SUCCESS")) {
                return new GatewayResponse(true, false, "00", "Captured", 90);
            }
            if (orderId.contains("FORCE_SOFT_FAIL")) {
                return new GatewayResponse(false, true, "GATEWAY_TIMEOUT", "Gateway timeout", 140);
            }
            if (orderId.contains("FORCE_HARD_FAIL")) {
                return new GatewayResponse(false, false, "INSUFFICIENT_FUNDS", "Customer has insufficient funds", 120);
            }
        }

        int bucket = Math.abs(orderId.hashCode()) % 100;
        int latencyMs = 60 + (Math.abs(orderId.hashCode()) % 490);

        if (bucket < 68) {
            return new GatewayResponse(true, false, "00", "Captured", latencyMs);
        }
        if (bucket < 90) {
            return new GatewayResponse(false, true, "GATEWAY_TIMEOUT", "Gateway timeout", latencyMs);
        }
        return new GatewayResponse(false, false, "INSUFFICIENT_FUNDS", "Customer has insufficient funds", latencyMs);
    }
}
