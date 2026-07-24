package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record MealVoucherRequestDTO(
        UUID employeeId,
        BigDecimal mealValue,
        Integer workingDays
) {
}
