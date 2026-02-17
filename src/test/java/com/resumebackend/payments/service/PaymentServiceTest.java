package com.resumebackend.payments.service;

import com.resumebackend.payments.domain.PaymentStatus;
import com.resumebackend.payments.dto.CreatePaymentIntentRequest;
import com.resumebackend.payments.dto.PaymentDetailResponse;
import com.resumebackend.payments.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MerchantRepository merchantRepository;

    private Long merchantId;

    @BeforeEach
    void setup() {
        merchantId = merchantRepository.findByApiKey("m_demo_merchant_key").orElseThrow().getId();
    }

    @Test
    void shouldBeIdempotentForSameIdempotencyKey() {
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
                "ORD-FORCE_SUCCESS-101",
                new BigDecimal("499.00"),
                "INR",
                "idem-101",
                "https://merchant.test/webhook",
                3
        );

        PaymentDetailResponse first = paymentService.createAndProcess(merchantId, request);
        PaymentDetailResponse second = paymentService.createAndProcess(merchantId, request);

        assertThat(first.payment().id()).isEqualTo(second.payment().id());
        assertThat(second.payment().status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void shouldScheduleRetryOnTransientFailure() {
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
                "ORD-FORCE_SOFT_FAIL-RETRY",
                new BigDecimal("799.00"),
                "INR",
                "idem-retry-1",
                "https://merchant.test/webhook",
                3
        );

        PaymentDetailResponse result = paymentService.createAndProcess(merchantId, request);

        assertThat(result.payment().status()).isEqualTo(PaymentStatus.RETRY_SCHEDULED);
        assertThat(result.payment().attemptCount()).isEqualTo(1);
        assertThat(result.payment().nextRetryAt()).isNotNull();
    }
}
