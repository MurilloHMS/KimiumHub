package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record TransportationVoucherResponseDTO(
        UUID employeeId,
        String employeeName,
        Integer dailyCommutesCount,
        BigDecimal fareValue,
        Integer workingDays,
        BigDecimal totalAmount
) {
}
