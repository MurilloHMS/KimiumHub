package com.proautokimium.api.Application.DTOs.humanResources.PositionLevel;

import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePositionLevelRequestDTO(
        String name,
        Integer levelOrder,
        UUID positionId,
        SalaryAdjustmentType adjustmentType,
        BigDecimal fixedAmount,
        BigDecimal percentageIncrease
) {
}
