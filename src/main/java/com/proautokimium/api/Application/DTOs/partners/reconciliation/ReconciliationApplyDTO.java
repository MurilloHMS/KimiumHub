package com.proautokimium.api.Application.DTOs.partners.reconciliation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReconciliationApplyDTO(
        @NotEmpty @Valid List<ReconciliationChoiceDTO> choices
) { }
