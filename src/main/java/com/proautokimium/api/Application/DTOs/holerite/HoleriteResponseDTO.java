package com.proautokimium.api.Application.DTOs.holerite;

import com.proautokimium.api.domain.enums.HoleriteTipo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record HoleriteResponseDTO(
        UUID id,
        LocalDate competencia,
        HoleriteTipo tipo,
        String originalFilename,
        LocalDateTime createdAt
) {
}
