package com.proautokimium.api.Application.DTOs.humanResources.CollectiveBargainingAdjustment;

import com.proautokimium.api.domain.enums.humanResources.AdjustmentScope;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ApplyCollectiveBargainingAdjustmentRequestDTO(
        BigDecimal percentage,
        LocalDate effectiveDate,
        AdjustmentScope scope,
        UUID positionId
) {
}
