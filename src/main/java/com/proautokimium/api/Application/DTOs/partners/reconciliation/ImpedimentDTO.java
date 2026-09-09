package com.proautokimium.api.Application.DTOs.partners.reconciliation;

import com.proautokimium.api.domain.enums.Impediments;

public record ImpedimentDTO(
        Impediments reason,
        String detail
) {
}
