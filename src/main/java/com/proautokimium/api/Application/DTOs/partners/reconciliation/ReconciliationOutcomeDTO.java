package com.proautokimium.api.Application.DTOs.partners.reconciliation;

import com.proautokimium.api.domain.enums.ReconciliationOutcome;

/** O que aconteceu com uma linha que não deu no esperado. */
public record ReconciliationOutcomeDTO(
        String code,
        String name,
        ReconciliationOutcome outcome,
        String detail
) { }
