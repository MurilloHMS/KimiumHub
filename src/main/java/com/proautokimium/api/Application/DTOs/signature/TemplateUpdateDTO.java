package com.proautokimium.api.Application.DTOs.signature;

import jakarta.validation.constraints.NotBlank;

public record TemplateUpdateDTO(
        @NotBlank String document
) {
}
