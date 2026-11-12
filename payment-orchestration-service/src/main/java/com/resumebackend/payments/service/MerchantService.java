package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.Merchant;
import com.resumebackend.payments.dto.CreateMerchantRequest;
import com.resumebackend.payments.dto.MerchantResponse;
import com.resumebackend.payments.exception.BadRequestException;
import com.resumebackend.payments.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Transactional
    public MerchantResponse create(CreateMerchantRequest request) {
        String apiKey = "m_" + UUID.randomUUID().toString().replace("-", "");

        while (merchantRepository.findByApiKey(apiKey).isPresent()) {
            apiKey = "m_" + UUID.randomUUID().toString().replace("-", "");
        }

        Merchant merchant = Merchant.builder()
                .name(request.name().trim())
                .apiKey(apiKey)
                .build();

        Merchant saved = merchantRepository.save(merchant);
        return new MerchantResponse(saved.getId(), saved.getName(), saved.getApiKey());
    }

    @Transactional(readOnly = true)
    public Merchant findById(Long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BadRequestException("Merchant does not exist"));
    }
}
