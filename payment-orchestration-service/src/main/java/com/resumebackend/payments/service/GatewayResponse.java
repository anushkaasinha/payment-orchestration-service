package com.resumebackend.payments.service;

public record GatewayResponse(boolean success, boolean transientFailure, String code, String message, int latencyMs) {
}
