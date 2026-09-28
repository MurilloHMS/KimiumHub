package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.UUID;

public record CreateVacationByRhDTO(
        @NotNull UUID employeeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        // O saldo DEPOIS do lançamento, gravado como está. Nulo = o sistema desconta.
        @PositiveOrZero(message = "O saldo de férias não pode ser negativo") Integer vacationBalanceDays,
        String notes
        ) {}
