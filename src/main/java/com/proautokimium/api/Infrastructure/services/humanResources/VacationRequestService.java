package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.ReviewVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.InsufficientVacationBalanceException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.OverlappingVacationRequestException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.VacationRequestNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VacationRequestService {

    private final VacationRequestRepository vacationRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public VacationRequestService(
            VacationRequestRepository vacationRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
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
    public VacationRequestResponseDTO approve(UUID id, ReviewVacationRequestDTO dto) {
        VacationRequest request = vacationRequestRepository.findById(id)
                .orElseThrow(VacationRequestNotFoundException::new);
        Employee reviewer = employeeRepository.findById(dto.reviewerId())
                .orElseThrow(EmployeeNotFoundException::new);

        request.approve(reviewer, dto.notes(), LocalDateTime.now(clock));

        Employee employee = request.getEmployee();
        int balance = employee.getVacationBalanceDays() != null ? employee.getVacationBalanceDays() : 0;
        employee.setVacationBalanceDays(balance - (int) request.getDaysRequested());
        employeeRepository.save(employee);

        VacationRequest saved = vacationRequestRepository.save(request);
        return toResponse(saved);
    }

    @Transactional
    public VacationRequestResponseDTO reject(UUID id, ReviewVacationRequestDTO dto) {
        VacationRequest request = vacationRequestRepository.findById(id)
                .orElseThrow(VacationRequestNotFoundException::new);
        Employee reviewer = employeeRepository.findById(dto.reviewerId())
                .orElseThrow(EmployeeNotFoundException::new);

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

    /** "Minhas solicitações" — resolve o funcionário pelo login autenticado, mesmo padrão dos outros módulos self-service. */
    public List<VacationRequestResponseDTO> listMine(String login) {
        Employee employee = resolveEmployee(login);
        if (employee == null) return List.of();

        return vacationRequestRepository.findByEmployeeOrderByRequestedAtDesc(employee).stream()
                .map(this::toResponse)
                .toList();
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
