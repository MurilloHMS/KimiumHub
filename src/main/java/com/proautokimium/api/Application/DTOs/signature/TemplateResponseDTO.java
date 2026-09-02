package com.proautokimium.api.Application.DTOs.signature;

import java.time.LocalDateTime;

public record TemplateResponseDTO(
        String document,
        LocalDateTime updatedAt,
        String updatedBy
) {
}
