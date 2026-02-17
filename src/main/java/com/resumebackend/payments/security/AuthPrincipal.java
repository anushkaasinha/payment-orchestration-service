package com.resumebackend.payments.security;

import lombok.Getter;

@Getter
public class AuthPrincipal {
    private final Long merchantId;
    private final String role;

    public AuthPrincipal(Long merchantId, String role) {
        this.merchantId = merchantId;
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
