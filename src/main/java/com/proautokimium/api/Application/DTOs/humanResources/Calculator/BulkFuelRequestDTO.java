package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BulkFuelRequestDTO(
        @NotNull @DecimalMin("0.01") BigDecimal fuelPricePerLiter,
        @NotNull @Min(1) Integer workingDays
) {}
