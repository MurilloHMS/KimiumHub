package com.proautokimium.api.Application.DTOs.sankhya;

import jakarta.validation.constraints.NotBlank;

public record SankhyaQueryDTO(@NotBlank  String query) {
}
