package com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeDocumentResponseDTO(
        UUID id,
        UUID employeeId,
        String title,
        String originalFilename,
        LocalDateTime uploadedAt
) {
}
