package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import java.time.LocalDate;
import java.util.UUID;

public record VacationAlertDTO(
        UUID employeeId,
        String employeeName,
        LocalDate hiringDate,
        int completedPeriods,
        int totalEarnedDays,
        int totalUsedDays,
        int balanceDays,
        LocalDate concessionDeadline,
        String alertLevel
) {}
