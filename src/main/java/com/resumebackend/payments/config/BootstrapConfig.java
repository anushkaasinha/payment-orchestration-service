package com.resumebackend.payments.config;

import com.resumebackend.payments.domain.Merchant;
import com.resumebackend.payments.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BootstrapConfig {

    private final MerchantRepository merchantRepository;

    @Bean
    CommandLineRunner seedMerchant() {
        return args -> {
            if (merchantRepository.count() == 0) {
                merchantRepository.save(Merchant.builder()
                        .name("Demo Merchant")
                        .apiKey("m_demo_merchant_key")
                        .build());
            }
        };
    }
}
