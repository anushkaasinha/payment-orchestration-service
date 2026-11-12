package com.resumebackend.payments.service;

import com.resumebackend.payments.controller.Dtos.MerchantResponse;
import com.resumebackend.payments.domain.Merchant;
import com.resumebackend.payments.repository.MerchantRepository;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class MerchantService {
    private final MerchantRepository merchants;
    private final SecureRandom random = new SecureRandom();

    public MerchantService(MerchantRepository merchants) { this.merchants = merchants; }

    public MerchantResponse create(String name) {
        Merchant m = new Merchant();
        m.setName(name);
        m.setApiKey("mk_" + token(16));
        m.setWebhookSecret("whsec_" + token(16));
        m = merchants.save(m);
        return new MerchantResponse(m.getId(), m.getName(), m.getApiKey(), m.getCreatedAt());
    }

    private String token(int bytes) {
        byte[] b = new byte[bytes];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
