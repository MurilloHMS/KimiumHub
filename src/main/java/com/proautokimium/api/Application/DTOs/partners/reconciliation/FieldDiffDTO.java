package com.proautokimium.api.Application.DTOs.partners.reconciliation;

public record FieldDiffDTO(
        String field,
        String localValue,
        String erpValue
) { }
