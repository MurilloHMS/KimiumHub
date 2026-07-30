package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateVacationByRhDTO(
        @NotNull UUID employeeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Integer vacationBalanceDays,
        String notes
        ) {}
