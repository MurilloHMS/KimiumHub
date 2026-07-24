package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record FuelRequestDTO(
        UUID employeeId,
        BigDecimal distanceKm,
        BigDecimal vehicleConsumptionKmPerLiter,
        BigDecimal literPrice
) {
}
