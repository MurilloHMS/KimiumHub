package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import java.time.LocalDate;
import java.util.UUID;

public record CreateVacationRequestDTO(
        UUID employeeId,
        LocalDate startDate,
        LocalDate endDate,
        UUID replacementEmployeeId
) {
}
