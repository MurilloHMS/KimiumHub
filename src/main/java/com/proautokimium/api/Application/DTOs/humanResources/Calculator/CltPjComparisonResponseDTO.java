package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record CltPjComparisonResponseDTO(
        UUID employeeId,
        String employeeName,
        BigDecimal baseSalary,
        BigDecimal inssPatronal,
        BigDecimal fgts,
        BigDecimal thirteenthSalaryProvision,
        BigDecimal vacationProvision,
        BigDecimal totalCltCost,
        BigDecimal pjEquivalentValue
) {
}
