package com.resumebackend.payments.service;

import com.resumebackend.payments.exception.UnauthorizedException;
import com.resumebackend.payments.security.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentAuthService {

    public Long merchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal) || principal.getMerchantId() == null) {
            throw new UnauthorizedException("Merchant authentication required");
        }
        return principal.getMerchantId();
    }
}
