package com.resumebackend.payments;

import com.jayway.jsonpath.JsonPath;
import com.resumebackend.payments.domain.GatewayName;
import com.resumebackend.payments.domain.WebhookEvent;
import com.resumebackend.payments.domain.WebhookStatus;
import com.resumebackend.payments.gateway.GatewayMode;
import com.resumebackend.payments.gateway.GatewayRegistry;
import com.resumebackend.payments.repository.WebhookEventRepository;
import com.resumebackend.payments.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "payments.scheduling.enabled=false")
@AutoConfigureMockMvc
class PaymentFlowTests {
    static final String MERCHANT = "m_demo_merchant_key";
    static final String ADMIN = "admin_secret_123";

    @Autowired MockMvc mvc;
    @Autowired GatewayRegistry registry;
    @Autowired WebhookService webhookService;
    @Autowired WebhookEventRepository events;

    @BeforeEach
    void resetGateways() { registry.reset(); }

    private ResultActions pay(String idem, String amount, int maxRetries, String webhook) throws Exception {
        String hook = webhook == null ? "" : ",\"webhookUrl\":\"" + webhook + "\"";
        return mvc.perform(post("/api/v1/merchant/payments").header("X-API-KEY", MERCHANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"ORD-1\",\"amount\":" + amount + ",\"currency\":\"INR\",\"idempotencyKey\":\"" + idem
                        + "\",\"maxRetries\":" + maxRetries + hook + "}"));
    }

    private ResultActions pay(String amount, int maxRetries) throws Exception {
        return pay(UUID.randomUUID().toString(), amount, maxRetries, null);
    }

    private static long idOf(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void rejectsMissingAndWrongRoleKeys() throws Exception {
        mvc.perform(get("/api/v1/merchant/payments/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/webhooks/dead-letter").header("X-API-KEY", MERCHANT)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/webhooks/dead-letter").header("X-ADMIN-KEY", ADMIN)).andExpect(status().isOk());
    }

    @Test
    void successfulPaymentUsesFirstGateway() throws Exception {
        pay("1299.00", 3).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.gateway").value("PAYU"))
                .andExpect(jsonPath("$.attempts.length()").value(1));
    }

    @Test
    void sameIdempotencyKeyReturnsSamePaymentWithoutReprocessing() throws Exception {
        String key = UUID.randomUUID().toString();
        long first = idOf(pay(key, "500.00", 3, null).andExpect(status().isCreated()));
        ResultActions second = pay(key, "500.00", 3, null)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.attempts.length()").value(1));
        assertEquals(first, idOf(second));
    }

    @Test
    void reusingKeyWithDifferentPayloadIsConflict() throws Exception {
        String key = UUID.randomUUID().toString();
        pay(key, "500.00", 3, null).andExpect(status().isCreated());
        pay(key, "999.00", 3, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void failsOverToNextGatewayWhenPrimaryIsDown() throws Exception {
        mvc.perform(put("/api/v1/admin/gateways/PAYU").header("X-ADMIN-KEY", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"DOWN\"}")).andExpect(status().isOk());
        pay("700.00", 3).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.gateway").value("PAYTM"))
                .andExpect(jsonPath("$.attempts.length()").value(2))
                .andExpect(jsonPath("$.attempts[0].outcome").value("TRANSIENT_FAILURE"));
    }

    @Test
    void allGatewaysDownSchedulesRetryThenExhausts() throws Exception {
        for (GatewayName g : GatewayName.values()) registry.setMode(g, GatewayMode.DOWN);
        pay("100.00", 3).andExpect(jsonPath("$.status").value("RETRY_SCHEDULED"))
                .andExpect(jsonPath("$.retryCount").value(1));
        pay("100.00", 0).andExpect(jsonPath("$.status").value("EXHAUSTED"));
    }

    @Test
    void hardDeclineDoesNotFailOverAndForceRetryRecovers() throws Exception {
        registry.setMode(GatewayName.PAYU, GatewayMode.DECLINE);
        long id = idOf(pay("250.00", 3).andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.attempts.length()").value(1)));
        registry.setMode(GatewayName.PAYU, GatewayMode.UP);
        mvc.perform(post("/api/v1/merchant/payments/" + id + "/force-retry").header("X-API-KEY", MERCHANT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(post("/api/v1/merchant/payments/" + id + "/force-retry").header("X-API-KEY", MERCHANT))
                .andExpect(status().isConflict());
    }

    @Test
    void validationErrorsAreCentralised() throws Exception {
        pay("-5", 3).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void ledgerBalancesAndReconciles() throws Exception {
        pay("1000.00", 3).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/admin/reconciliation/1").header("X-ADMIN-KEY", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.successWithoutLedger.length()").value(0));
    }

    @Test
    void undeliverableWebhookEndsInDeadLetter() throws Exception {
        long id = idOf(pay(UUID.randomUUID().toString(), "300.00", 3, "http://127.0.0.1:1/hook")
                .andExpect(status().isCreated()));
        WebhookEvent e = events.findAll().stream().filter(x -> x.getPaymentId().equals(id)).findFirst().orElseThrow();
        for (int i = 0; i < 5; i++) webhookService.deliver(e.getId());
        assertEquals(WebhookStatus.DEAD_LETTER, events.findById(e.getId()).orElseThrow().getStatus());
        mvc.perform(get("/api/v1/admin/webhooks/dead-letter").header("X-ADMIN-KEY", ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.paymentId==" + id + ")]").exists());
    }
}
