package com.proautokimium.api.Application.DTOs.humanResources.Calendar;

import com.proautokimium.api.domain.enums.humanResources.CalendarEventType;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;

import java.time.LocalDate;
import java.util.UUID;

public record CalendarEventDTO(
        UUID id,
        CalendarEventType eventType,
        UUID employeeId,
        String employeeName,
        UUID teamId,
        String teamName,
        LocalDate startDate,
        LocalDate endDate,
        VacationRequestStatus status
) {
}
