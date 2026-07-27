package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record BulkFuelResultDTO(
        UUID employeeId,
        String employeeName,
        String document,
        BigDecimal dailyDistanceKm,
        BigDecimal vehicleKmPerLiter,
        BigDecimal litersNeeded,
        BigDecimal totalAmount
) {}
