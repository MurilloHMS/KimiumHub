package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.CareerHistory.CareerHistoryResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CareerHistoryService {

    private final CareerHistoryRepository careerHistoryRepository;
    private final EmployeeRepository employeeRepository;

    public CareerHistoryService(CareerHistoryRepository careerHistoryRepository, EmployeeRepository employeeRepository) {
        this.careerHistoryRepository = careerHistoryRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<CareerHistoryResponseDTO> listByEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        return careerHistoryRepository.findByEmployeeOrderByEffectiveDateDesc(employee).stream()
                .map(this::toResponse)
                .toList();
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
