package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record FuelResponseDTO(
        UUID employeeId,
        String employeeName,
        BigDecimal distanceKm,
        BigDecimal litersNeeded,
        BigDecimal literPrice,
        BigDecimal totalAmount
) {
}
