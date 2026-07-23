package com.proautokimium.api.Application.DTOs.humanResources.CareerHistory;

import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.enums.humanResources.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CareerHistoryResponseDTO(
        UUID id,
        UUID employeeId,
        UUID positionId,
        UUID positionLevelId,
        BigDecimal salary,
        ContractType contractType,
        CareerChangeReason reason,
        LocalDate effectiveDate,
        String notes
) {
}
