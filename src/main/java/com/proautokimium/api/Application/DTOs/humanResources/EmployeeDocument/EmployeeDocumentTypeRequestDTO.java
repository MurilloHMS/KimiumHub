package com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record EmployeeDocumentTypeRequestDTO(
        @NotBlank(message = "Informe o nome do tipo de documento.") @Size(max = 100) String name,
        List<Integer> alertDaysBefore,
        boolean notifyOnExpiry,
        List<UUID> recipientEmployeeIds,
        boolean active
) {
}
