package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.TeamOverview.TeamOverviewEntryDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.AvailabilityStatus;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamOverviewService {

    private final EmployeeRepository employeeRepository;
    private final CareerHistoryRepository careerHistoryRepository;
    private final VacationRequestRepository vacationRequestRepository;
    private final Clock clock;

    public TeamOverviewService(
            EmployeeRepository employeeRepository,
            CareerHistoryRepository careerHistoryRepository,
            VacationRequestRepository vacationRequestRepository,
            Clock clock
    ) {
        this.employeeRepository = employeeRepository;
        this.careerHistoryRepository = careerHistoryRepository;
        this.vacationRequestRepository = vacationRequestRepository;
        this.clock = clock;
    }

    /** Painel do RH: cada funcionário ativo, com CLT/PJ e disponibilidade (em férias / agendada / disponível). */
    public List<TeamOverviewEntryDTO> getOverview(UUID teamId, UUID companyId) {
        LocalDate today = LocalDate.now(clock);

        List<Employee> employees = employeeRepository.findByAtivoTrue().stream()
                .filter(e -> teamId == null || (e.getTeam() != null && e.getTeam().getId().equals(teamId)))
                .filter(e -> companyId == null || (e.getCompany() != null && e.getCompany().getId().equals(companyId)))
                .toList();

        Map<UUID, ContractType> contractTypeByEmployee = careerHistoryRepository.findLatestPerEmployee().stream()
                .collect(Collectors.toMap(ch -> ch.getEmployee().getId(), CareerHistory::getContractType));

        Map<UUID, List<VacationRequest>> approvedVacationsByEmployee = vacationRequestRepository
                .findByStatus(VacationRequestStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(vr -> vr.getEmployee().getId()));

        return employees.stream()
                .map(employee -> toEntry(employee, today, contractTypeByEmployee, approvedVacationsByEmployee))
                .toList();
    }

    private TeamOverviewEntryDTO toEntry(
            Employee employee, LocalDate today,
            Map<UUID, ContractType> contractTypeByEmployee,
            Map<UUID, List<VacationRequest>> approvedVacationsByEmployee
    ) {
        List<VacationRequest> approved = approvedVacationsByEmployee.getOrDefault(employee.getId(), List.of());

        return new TeamOverviewEntryDTO(
                employee.getId(),
                employee.getName(),
                employee.getTeam() != null ? employee.getTeam().getId() : null,
                employee.getTeam() != null ? employee.getTeam().getName() : null,
                employee.getCompany() != null ? employee.getCompany().getId() : null,
                employee.getCompany() != null ? employee.getCompany().getName() : null,
                contractTypeByEmployee.get(employee.getId()),
                resolveStatus(approved, today)
        );
    }

    /** Prioridade: em férias agora > férias agendada > disponível. */
    private AvailabilityStatus resolveStatus(List<VacationRequest> approvedRequests, LocalDate today) {
        boolean onVacation = approvedRequests.stream()
                .anyMatch(vr -> !today.isBefore(vr.getStartDate()) && !today.isAfter(vr.getEndDate()));
        if (onVacation) return AvailabilityStatus.ON_VACATION;

        boolean scheduled = approvedRequests.stream().anyMatch(vr -> today.isBefore(vr.getStartDate()));
        if (scheduled) return AvailabilityStatus.VACATION_SCHEDULED;

        return AvailabilityStatus.AVAILABLE;
    }
}
