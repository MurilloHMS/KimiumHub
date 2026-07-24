package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.DeliverEquipmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.EquipmentAssignmentResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.ReturnEquipmentRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EquipmentAssignmentRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EquipmentAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentAssignmentServiceTest {

    @Mock private EquipmentAssignmentRepository repository;
    @Mock private EmployeeRepository employeeRepository;

    private EquipmentAssignmentService service;
    private UUID employeeId;
    private Employee employee;

    @BeforeEach
    void setUp() throws Exception {
        service = new EquipmentAssignmentService(repository, employeeRepository);

        employeeId = UUID.randomUUID();
        employee = new Employee();
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(employee, employeeId);
    }

    @Test
    @DisplayName("Deve registrar a entrega do equipamento")
    void deveRegistrarEntrega() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(repository.save(any(EquipmentAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliverEquipmentRequestDTO dto = new DeliverEquipmentRequestDTO(
                employeeId, "Notebook", "Dell Latitude - SN123", LocalDate.of(2026, 7, 1), null
        );

        EquipmentAssignmentResponseDTO response = service.deliver(dto);

        assertThat(response.withEmployee()).isTrue();
        assertThat(response.equipmentType()).isEqualTo("Notebook");
    }

    @Test
    @DisplayName("Deve marcar como devolvido")
    void deveMarcarComoDevolvido() {
        EquipmentAssignment assignment = EquipmentAssignment.deliver(
                employee, "Notebook", "Dell Latitude", LocalDate.of(2026, 7, 1), null
        );
        UUID assignmentId = UUID.randomUUID();

        when(repository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(repository.save(any(EquipmentAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentAssignmentResponseDTO response = service.markAsReturned(
                assignmentId, new ReturnEquipmentRequestDTO(LocalDate.of(2026, 7, 20))
        );

        assertThat(response.withEmployee()).isFalse();
        assertThat(response.returnedAt()).isEqualTo(LocalDate.of(2026, 7, 20));
    }
}
