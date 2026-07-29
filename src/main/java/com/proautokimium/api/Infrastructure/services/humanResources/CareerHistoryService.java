package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.CareerHistory.CareerHistoryResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.CareerHistory.CreateCareerHistoryDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionLevelNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionLevelRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CareerHistoryService {

    private final CareerHistoryRepository careerHistoryRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final PositionLevelRepository positionLevelRepository;
    private final PositionLevelSalaryResolver salaryResolver;

    public CareerHistoryService(CareerHistoryRepository careerHistoryRepository,
                                EmployeeRepository employeeRepository,
                                PositionRepository positionRepository,
                                PositionLevelRepository positionLevelRepository,
                                PositionLevelSalaryResolver salaryResolver) {
        this.careerHistoryRepository = careerHistoryRepository;
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
        this.positionLevelRepository = positionLevelRepository;
        this.salaryResolver = salaryResolver;
    }

    public List<CareerHistoryResponseDTO> listByEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        return careerHistoryRepository.findByEmployeeOrderByEffectiveDateDesc(employee).stream()
                .map(this::toResponse)
                .toList();
    }

    public CareerHistoryResponseDTO create(CreateCareerHistoryDTO dto) {
        Employee employee = employeeRepository.findById(dto.employeeId())
                .orElseThrow(EmployeeNotFoundException::new);

        Position position = positionRepository.findById(dto.positionId())
                .orElseThrow(PositionNotFoundException::new);

        PositionLevel level = positionLevelRepository.findById(dto.positionLevelId())
                .orElseThrow(PositionLevelNotFoundException::new);

        var salary = salaryResolver.resolve(level);

        var history = new CareerHistory(
                employee, position, level, salary,
                dto.contractType(), dto.reason(), dto.effectiveDate(), dto.notes()
        );

        return toResponse(careerHistoryRepository.save(history));
    }

    private CareerHistoryResponseDTO toResponse(CareerHistory history) {
        return new CareerHistoryResponseDTO(
                history.getId(),
                history.getEmployee().getId(),
                history.getPosition().getId(),
                history.getPositionLevel().getId(),
                history.getSalary(),
                history.getContractType(),
                history.getReason(),
                history.getEffectiveDate(),
                history.getNotes()
        );
    }
}
