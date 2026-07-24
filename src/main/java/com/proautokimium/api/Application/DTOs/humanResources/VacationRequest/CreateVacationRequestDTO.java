package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import java.time.LocalDate;
import java.util.UUID;

/** employeeId não entra aqui de propósito — quem solicita é sempre o funcionário autenticado. */
public record CreateVacationRequestDTO(
        LocalDate startDate,
        LocalDate endDate,
        UUID replacementEmployeeId
) {
}
