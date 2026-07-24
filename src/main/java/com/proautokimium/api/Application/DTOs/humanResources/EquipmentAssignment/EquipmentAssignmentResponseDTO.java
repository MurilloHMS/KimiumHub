package com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment;

import java.time.LocalDate;
import java.util.UUID;

public record EquipmentAssignmentResponseDTO(
        UUID id,
        UUID employeeId,
        String equipmentType,
        String description,
        LocalDate deliveredAt,
        LocalDate returnedAt,
        String notes,
        boolean withEmployee
) {
}
