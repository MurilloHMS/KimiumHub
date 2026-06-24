package com.proautokimium.api.Application.DTOs.holerite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record HoleriteResponseDTO(
        UUID id,
        LocalDate competencia,
        String originalFilename,
        LocalDateTime createdAt
) {
}
