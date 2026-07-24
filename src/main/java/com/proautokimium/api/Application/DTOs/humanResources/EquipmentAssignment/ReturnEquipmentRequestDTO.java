package com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment;

import java.time.LocalDate;

public record ReturnEquipmentRequestDTO(
        LocalDate returnedAt
) {
}
