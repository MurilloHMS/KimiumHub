package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calendar.CalendarEventDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.CalendarEventType;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CalendarService {

    private final VacationRequestRepository vacationRequestRepository;

    public CalendarService(VacationRequestRepository vacationRequestRepository) {
        this.vacationRequestRepository = vacationRequestRepository;
    }

    /** Eventos do calendário do RH que se sobrepõem ao período informado. Status default: APPROVED. */
    public List<CalendarEventDTO> getEvents(LocalDate start, LocalDate end, UUID teamId, UUID companyId, VacationRequestStatus status) {
        VacationRequestStatus effectiveStatus = status != null ? status : VacationRequestStatus.APPROVED;

        return vacationRequestRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(effectiveStatus, end, start).stream()
                .filter(vr -> teamId == null || matchesTeam(vr.getEmployee(), teamId))
                .filter(vr -> companyId == null || matchesCompany(vr.getEmployee(), companyId))
                .map(this::toEvent)
                .toList();
    }

    private boolean matchesTeam(Employee employee, UUID teamId) {
        return employee.getTeam() != null && employee.getTeam().getId().equals(teamId);
    }

    private boolean matchesCompany(Employee employee, UUID companyId) {
        return employee.getCompany() != null && employee.getCompany().getId().equals(companyId);
    }

    private CalendarEventDTO toEvent(VacationRequest vr) {
        Employee employee = vr.getEmployee();
        return new CalendarEventDTO(
                vr.getId(),
                CalendarEventType.VACATION,
                employee.getId(),
                employee.getName(),
                employee.getTeam() != null ? employee.getTeam().getId() : null,
                employee.getTeam() != null ? employee.getTeam().getName() : null,
                vr.getStartDate(),
                vr.getEndDate(),
                vr.getStatus()
        );
    }
}
