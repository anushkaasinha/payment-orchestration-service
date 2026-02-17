package com.resumebackend.payments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumebackend.payments.dto.CreateMerchantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectAdminEndpointWithoutKey() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMerchantRequest("Acme"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateMerchantWithAdminKey() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchants")
                        .header("X-ADMIN-KEY", "admin_secret_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMerchantRequest("Acme Payments"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey").exists())
                .andExpect(jsonPath("$.name").value("Acme Payments"));
    }
}
