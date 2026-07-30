package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.*;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.InsufficientVacationBalanceException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.OverlappingVacationRequestException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.VacationRequestNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class VacationRequestService {

    private final VacationRequestRepository vacationRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final CareerHistoryRepository careerHistoryRepository;
    private final Clock clock;

    public VacationRequestService(
            VacationRequestRepository vacationRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            CareerHistoryRepository careerHistoryRepository,
            Clock clock
    ) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.careerHistoryRepository = careerHistoryRepository;
        this.clock = clock;
    }

    /** Quem solicita é sempre o funcionário autenticado — employeeId nunca vem do cliente. */
    @Transactional
    public VacationRequestResponseDTO create(CreateVacationRequestDTO dto, String login) {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new EmployeeNotFoundException();
        }

        Employee replacement = dto.replacementEmployeeId() != null
                ? employeeRepository.findById(dto.replacementEmployeeId()).orElseThrow(EmployeeNotFoundException::new)
                : null;

        VacationRequest request = VacationRequest.request(
                employee, dto.startDate(), dto.endDate(), replacement, LocalDateTime.now(clock)
        );

        int balance = employee.getVacationBalanceDays() != null ? employee.getVacationBalanceDays() : 0;
        if (request.getDaysRequested() > balance) {
            throw new InsufficientVacationBalanceException();
        }

        if (employee.getTeam() != null) {
            List<VacationRequest> overlapping = vacationRequestRepository.findOverlappingInTeam(
                    employee.getTeam(), employee, dto.startDate(), dto.endDate()
            );
            if (!overlapping.isEmpty()) {
                throw new OverlappingVacationRequestException();
            }
        }

        VacationRequest saved = vacationRequestRepository.save(request);
        return toResponse(saved);
    }

    @Transactional
    public VacationRequestResponseDTO approve(UUID id, ReviewVacationRequestDTO dto, String reviewerLogin) {
        VacationRequest request = vacationRequestRepository.findById(id)
                .orElseThrow(VacationRequestNotFoundException::new);
        Employee reviewer = resolveEmployee(reviewerLogin);

        request.approve(reviewer, dto.notes(), LocalDateTime.now(clock));

        Employee employee = request.getEmployee();
        int balance = employee.getVacationBalanceDays() != null ? employee.getVacationBalanceDays() : 0;
        employee.setVacationBalanceDays(balance - (int) request.getDaysRequested());
        employeeRepository.save(employee);

        VacationRequest saved = vacationRequestRepository.save(request);
        return toResponse(saved);
    }

    @Transactional
    public VacationRequestResponseDTO reject(UUID id, ReviewVacationRequestDTO dto, String reviewerLogin) {
        VacationRequest request = vacationRequestRepository.findById(id)
                .orElseThrow(VacationRequestNotFoundException::new);
        Employee reviewer = resolveEmployee(reviewerLogin);

        request.reject(reviewer, dto.notes(), LocalDateTime.now(clock));

        VacationRequest saved = vacationRequestRepository.save(request);
        return toResponse(saved);
    }

    public List<VacationRequestResponseDTO> listByEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        return vacationRequestRepository.findByEmployeeOrderByRequestedAtDesc(employee).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Gerenciador do RH — lista tudo, opcionalmente filtrado por status. */
    public List<VacationRequestResponseDTO> listAll(VacationRequestStatus status) {
        List<VacationRequest> results = status != null
                ? vacationRequestRepository.findByStatusOrderByRequestedAtDesc(status)
                : vacationRequestRepository.findAllByOrderByRequestedAtDesc();
        return results.stream().map(this::toResponse).toList();
    }

    /**
     * "Minhas férias" — saldo atual + histórico, resolvendo o funcionário pelo login
     * autenticado (mesmo padrão dos outros módulos self-service). O saldo precisa vir
     * junto porque é o dado que o funcionário usa pra decidir quantos dias pedir.
     */
    public EmployeeVacationOverviewDTO getMyOverview(String login) {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new EmployeeNotFoundException();
        }

        List<VacationRequestResponseDTO> requests = vacationRequestRepository
                .findByEmployeeOrderByRequestedAtDesc(employee).stream()
                .map(this::toResponse)
                .toList();

        return new EmployeeVacationOverviewDTO(employee.getVacationBalanceDays(), requests);
    }

    /**
     * CLT vacation alerts: for each active employee with a hiringDate, calculates
     * acquisition periods (12 months each), compares earned days vs approved days taken,
     * and returns alerts sorted by urgency.
     */
    public List<VacationAlertDTO> getVacationAlerts() {
        LocalDate today = LocalDate.now(clock);
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(Employee::isAtivo)
                .toList();

        // Build a map of employee -> hiring date from the earliest CareerHistory (HIRING reason)
        List<CareerHistory> allHistories = careerHistoryRepository.findAll();
        var hiringDates = new java.util.HashMap<UUID, LocalDate>();
        for (CareerHistory ch : allHistories) {
            if (ch.getReason() == CareerChangeReason.HIRING) {
                UUID empId = ch.getEmployee().getId();
                LocalDate existing = hiringDates.get(empId);
                if (existing == null || ch.getEffectiveDate().isBefore(existing)) {
                    hiringDates.put(empId, ch.getEffectiveDate());
                }
            }
        }

        List<VacationAlertDTO> alerts = new ArrayList<>();

        for (Employee emp : employees) {
            LocalDate hiringDate = hiringDates.get(emp.getId());
            if (hiringDate == null) continue;
            long monthsSinceHire = ChronoUnit.MONTHS.between(hiringDate, today);
            int completedPeriods = (int) (monthsSinceHire / 12);
            if (completedPeriods <= 0) continue;

            int totalEarnedDays = completedPeriods * 30;

            List<VacationRequest> approved = vacationRequestRepository
                    .findByEmployeeAndStatus(emp, VacationRequestStatus.APPROVED);
            int totalUsedDays = approved.stream()
                    .mapToInt(vr -> (int) vr.getDaysRequested())
                    .sum();

            int balanceDays = totalEarnedDays - totalUsedDays;
            if (balanceDays <= 0) continue;

            int periodsFullyRedeemed = totalUsedDays / 30;
            LocalDate concessionDeadline = hiringDate.plusMonths((long) (periodsFullyRedeemed + 2) * 12);

            long daysUntilDeadline = ChronoUnit.DAYS.between(today, concessionDeadline);

            String alertLevel;
            if (daysUntilDeadline < 0) {
                alertLevel = "EXPIRED";
            } else if (daysUntilDeadline <= 30) {
                alertLevel = "CRITICAL";
            } else if (daysUntilDeadline <= 90) {
                alertLevel = "WARNING";
            } else {
                alertLevel = "OK";
            }

            alerts.add(new VacationAlertDTO(
                    emp.getId(), emp.getName(), hiringDate,
                    completedPeriods, totalEarnedDays, totalUsedDays, balanceDays,
                    concessionDeadline, alertLevel
            ));
        }

        alerts.sort(Comparator.comparing(VacationAlertDTO::concessionDeadline));
        return alerts;
    }

    @Transactional
    public VacationRequestResponseDTO createByRh(CreateVacationByRhDTO dto, String login){
        Employee employee = employeeRepository.findById(dto.employeeId())
                .orElseThrow(EmployeeNotFoundException::new);

        Employee reviewer = resolveEmployee(login);

        if(dto.vacationBalanceDays() != null){
            employee.setVacationBalanceDays(dto.vacationBalanceDays());
        }

        if(employee.getTeam() != null){
            List<VacationRequest> overlapping = vacationRequestRepository.findOverlappingInTeam(
                    employee.getTeam(), employee, dto.startDate(), dto.endDate()
            );
            if(!overlapping.isEmpty()){
                throw new OverlappingVacationRequestException();
            }
        }

        VacationRequest request = VacationRequest.request(
                employee, dto.startDate(), dto.endDate(), null, LocalDateTime.now(clock)
        );

        request.approve(reviewer, dto.notes(), LocalDateTime.now(clock));

        int balance = employee.getVacationBalanceDays() != null ? employee.getVacationBalanceDays() : 0;
        employee.setVacationBalanceDays(balance - (int) request.getDaysRequested());
        employeeRepository.save(employee);

        VacationRequest saved = vacationRequestRepository.save(request);
        return toResponse(saved);
    }

    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(u -> u.getEmployee())
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    private VacationRequestResponseDTO toResponse(VacationRequest request) {
        return new VacationRequestResponseDTO(
                request.getId(),
                request.getEmployee().getId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getDaysRequested(),
                request.getReplacementEmployee() != null ? request.getReplacementEmployee().getId() : null,
                request.getStatus(),
                request.getRequestedAt(),
                request.getReviewedBy() != null ? request.getReviewedBy().getId() : null,
                request.getReviewedAt(),
                request.getReviewNotes()
        );
    }
}
