package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.DeliverEquipmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.EquipmentAssignmentResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EquipmentAssignment.ReturnEquipmentRequestDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.EquipmentAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/equipment-assignments")
@Tag(name = "Equipamentos", description = "Vínculo de equipamentos (celular, veículo etc.) aos funcionários")
@PreAuthorize("hasAnyRole('ADMIN', 'RH')")
public class EquipmentAssignmentController {

    private final EquipmentAssignmentService service;

    public EquipmentAssignmentController(EquipmentAssignmentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registra entrega", description = "Vincula um equipamento a um funcionário")
    public ResponseEntity<EquipmentAssignmentResponseDTO> deliver(@Valid @RequestBody DeliverEquipmentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.deliver(request));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Registra devolução", description = "Marca o equipamento como devolvido")
    public ResponseEntity<EquipmentAssignmentResponseDTO> markAsReturned(
            @PathVariable UUID id, @Valid @RequestBody ReturnEquipmentRequestDTO request) {
        return ResponseEntity.ok(service.markAsReturned(id, request));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Histórico do funcionário", description = "Todos os equipamentos já entregues/devolvidos por esse funcionário")
    public ResponseEntity<List<EquipmentAssignmentResponseDTO>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(service.listByEmployee(employeeId));
    }

    @GetMapping
    @Operation(summary = "Equipamentos em posse", description = "Tudo que está com algum funcionário agora, ainda não devolvido")
    public ResponseEntity<List<EquipmentAssignmentResponseDTO>> currentlyWithEmployees() {
        return ResponseEntity.ok(service.listCurrentlyWithEmployees());
    }
}
