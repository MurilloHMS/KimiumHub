package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.DeliverEquipmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.EquipmentAssignmentResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.ReturnEquipmentRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EquipmentAssignmentNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EquipmentAssignmentRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EquipmentAssignment;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EquipmentAssignmentService {

    private final EquipmentAssignmentRepository repository;
    private final EmployeeRepository employeeRepository;

    public EquipmentAssignmentService(
            EquipmentAssignmentRepository repository,
            EmployeeRepository employeeRepository
    ) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public EquipmentAssignmentResponseDTO deliver(DeliverEquipmentRequestDTO dto) {
        Employee employee = employeeRepository.findById(dto.employeeId())
                .orElseThrow(EmployeeNotFoundException::new);

        EquipmentAssignment assignment = EquipmentAssignment.deliver(
                employee, dto.equipmentType(), dto.description(), dto.deliveredAt(), dto.notes()
        );

        EquipmentAssignment saved = repository.save(assignment);
        return toResponse(saved);
    }

    @Transactional
    public EquipmentAssignmentResponseDTO markAsReturned(UUID id, ReturnEquipmentRequestDTO dto) {
        EquipmentAssignment assignment = repository.findById(id)
                .orElseThrow(EquipmentAssignmentNotFoundException::new);

        assignment.markAsReturned(dto.returnedAt());

        EquipmentAssignment saved = repository.save(assignment);
        return toResponse(saved);
    }

    public List<EquipmentAssignmentResponseDTO> listByEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        return repository.findByEmployeeOrderByDeliveredAtDesc(employee).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Tudo que está com algum funcionário agora (ainda não devolvido). */
    public List<EquipmentAssignmentResponseDTO> listCurrentlyWithEmployees() {
        return repository.findByReturnedAtIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    private EquipmentAssignmentResponseDTO toResponse(EquipmentAssignment assignment) {
        return new EquipmentAssignmentResponseDTO(
                assignment.getId(),
                assignment.getEmployee().getId(),
                assignment.getEquipmentType(),
                assignment.getDescription(),
                assignment.getDeliveredAt(),
                assignment.getReturnedAt(),
                assignment.getNotes(),
                assignment.isWithEmployee()
        );
    }
}
