package com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument;

import com.proautokimium.api.domain.enums.humanResources.EmployeeDocumentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um documento como a tela o mostra. `status` e `daysUntilDue` já vêm
 * calculados pela API — a regra mora num lugar só (`statusOn`), e a tela não
 * a reescreve em TypeScript.
 */
public record EmployeeDocumentResponseDTO(
        UUID id,
        UUID employeeId,
        String employeeName,
        UUID typeId,
        String typeName,
        String title,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        LocalDate dueDate,
        EmployeeDocumentStatus status,
        Long daysUntilDue,
        UUID replacedById,
        LocalDateTime uploadedAt,
        String uploadedBy
) {
}
