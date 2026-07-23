package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record VacationRequestResponseDTO(
        UUID id,
        UUID employeeId,
        LocalDate startDate,
        LocalDate endDate,
        long daysRequested,
        UUID replacementEmployeeId,
        VacationRequestStatus status,
        LocalDateTime requestedAt,
        UUID reviewedById,
        LocalDateTime reviewedAt,
        String reviewNotes
) {
}
