package com.resumebackend.payments.config;

import com.resumebackend.payments.domain.Merchant;
import com.resumebackend.payments.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {
    private final MerchantRepository merchants;
    private final String seedKey;

    public DataSeeder(MerchantRepository merchants, @Value("${payments.seed-merchant-key}") String seedKey) {
        this.merchants = merchants;
        this.seedKey = seedKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (merchants.findByApiKey(seedKey).isEmpty()) {
            Merchant m = new Merchant();
            m.setName("Demo Merchant");
            m.setApiKey(seedKey);
            m.setWebhookSecret("whsec_demo");
            merchants.save(m);
        }
    }
}
