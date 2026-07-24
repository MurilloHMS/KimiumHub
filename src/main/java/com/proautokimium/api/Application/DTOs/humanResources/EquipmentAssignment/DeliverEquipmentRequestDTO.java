package com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment;

import java.time.LocalDate;
import java.util.UUID;

public record DeliverEquipmentRequestDTO(
        UUID employeeId,
        String equipmentType,
        String description,
        LocalDate deliveredAt,
        String notes
) {
}
