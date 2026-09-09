package com.proautokimium.api.Application.DTOs.partners.reconciliation;

import java.util.List;

public record ReconciliationRowDTO(
        String code,
        String name,
        String document,
        String email,
        String matrizCode,
        boolean active,
        String signature,
        List<FieldDiffDTO> differences,
        List<ImpedimentDTO> impediments
) {
}
