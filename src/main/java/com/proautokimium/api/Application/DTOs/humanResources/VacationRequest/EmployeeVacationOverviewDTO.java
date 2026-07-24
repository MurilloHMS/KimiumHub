package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import java.util.List;

public record EmployeeVacationOverviewDTO(
        Integer vacationBalanceDays,
        List<VacationRequestResponseDTO> requests
) {
}
