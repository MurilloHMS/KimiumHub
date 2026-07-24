package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record MealVoucherResponseDTO(
        UUID employeeId,
        String employeeName,
        Integer dailyMealsCount,
        BigDecimal mealValue,
        Integer workingDays,
        BigDecimal totalAmount
) {
}
