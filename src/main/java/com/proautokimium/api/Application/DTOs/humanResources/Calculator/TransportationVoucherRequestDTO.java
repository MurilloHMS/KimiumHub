package com.proautokimium.api.Application.DTOs.humanResources.Calculator;

import java.math.BigDecimal;
import java.util.UUID;

public record TransportationVoucherRequestDTO(
        UUID employeeId,
        BigDecimal fareValue,
        Integer workingDays
) {
}
