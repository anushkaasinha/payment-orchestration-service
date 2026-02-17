package com.resumebackend.payments.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "payment-orchestration-service",
                "status", "up",
                "docs", "Use /api/v1/admin/* (X-ADMIN-KEY) and /api/v1/merchant/* (X-API-KEY)",
                "health", "/actuator/health"
        );
    }
}
