package com.proautokimium.api.Application.DTOs.humanResources.CareerHistory;

import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateCareerHistoryDTO(
        @NotNull UUID employeeId,
        @NotNull UUID positionId,
        @NotNull UUID positionLevelId,
        @NotNull ContractType contractType,
        @NotNull CareerChangeReason reason,
        @NotNull LocalDate effectiveDate,
        String notes
) {
}
