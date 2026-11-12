package com.resumebackend.payments.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMerchantRequest(@NotBlank String name) {
}
