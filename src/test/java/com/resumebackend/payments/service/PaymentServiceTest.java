package com.resumebackend.payments.service;

import com.resumebackend.payments.controller.Dtos.CreatePaymentRequest;
import com.resumebackend.payments.domain.GatewayName;
import com.resumebackend.payments.domain.PaymentStatus;
import com.resumebackend.payments.gateway.GatewayMode;
import com.resumebackend.payments.gateway.GatewayRegistry;
import com.resumebackend.payments.repository.MerchantRepository;
import org.junit.jupiter.api.AfterEach;
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

    @Autowired
    private GatewayRegistry gatewayRegistry;

    private Long merchantId;

    @BeforeEach
    void setup() {
        gatewayRegistry.reset();

        merchantId = merchantRepository.findByApiKey("m_demo_merchant_key")
                .orElseThrow()
                .getId();
    }

    @AfterEach
    void cleanup() {
        gatewayRegistry.reset();
    }

    @Test
    void shouldBeIdempotentForSameIdempotencyKey() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORD-FORCE_SUCCESS-101",
                new BigDecimal("499.00"),
                "INR",
                "idem-101",
                "https://merchant.test/webhook",
                3
        );

        PaymentService.CreateResult first =
                paymentService.create(merchantId, request);

        PaymentService.CreateResult second =
                paymentService.create(merchantId, request);

        assertThat(first.payment().id()).isEqualTo(second.payment().id());
        assertThat(first.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(second.replayed()).isTrue();
    }

    @Test
    void shouldScheduleRetryOnTransientFailure() {

        // Simulate an outage at every payment gateway.
        for (GatewayName gateway : GatewayName.values()) {
            gatewayRegistry.setMode(gateway, GatewayMode.DOWN);
        }

        CreatePaymentRequest request = new CreatePaymentRequest(
                "ORD-RETRY-TEST-101",
                new BigDecimal("799.00"),
                "INR",
                "idem-retry-1",
                "https://merchant.test/webhook",
                3
        );

        PaymentService.CreateResult result =
                paymentService.create(merchantId, request);

        assertThat(result.payment().status())
                .isEqualTo(PaymentStatus.RETRY_SCHEDULED);

        assertThat(result.payment().nextRetryAt())
                .isNotNull();
    }
}
