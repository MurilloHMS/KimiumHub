package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.*;
import com.proautokimium.api.Infrastructure.services.humanResources.VacationRequestService;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/vacation-requests")
public class VacationRequestController {

    private final VacationRequestService vacationRequestService;

    public VacationRequestController(VacationRequestService vacationRequestService) {
        this.vacationRequestService = vacationRequestService;
    }

    @PreAuthorize("hasAuthority('documentos/rh/vacation-requests:INCLUIR')")
    @PostMapping
    public ResponseEntity<VacationRequestResponseDTO> create(
            @Valid @RequestBody CreateVacationRequestDTO request, Authentication auth) {
        return ResponseEntity.ok(vacationRequestService.create(request, auth.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('rh/vacation-requests:ALTERAR')")
    public ResponseEntity<VacationRequestResponseDTO> approve(
            @PathVariable UUID id, @Valid @RequestBody ReviewVacationRequestDTO request, Authentication auth) {
        return ResponseEntity.ok(vacationRequestService.approve(id, request, auth.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('rh/vacation-requests:ALTERAR')")
    public ResponseEntity<VacationRequestResponseDTO> reject(
            @PathVariable UUID id, @Valid @RequestBody ReviewVacationRequestDTO request, Authentication auth) {
        return ResponseEntity.ok(vacationRequestService.reject(id, request, auth.getName()));
    }

    @PreAuthorize("hasAuthority('documentos/rh/vacation-requests:CONSULTAR')")
    @GetMapping("/me")
    public ResponseEntity<EmployeeVacationOverviewDTO> mine(Authentication auth) {
        return ResponseEntity.ok(vacationRequestService.getMyOverview(auth.getName()));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('rh/vacation-requests:CONSULTAR')")
    public ResponseEntity<List<VacationAlertDTO>> alerts() {
        return ResponseEntity.ok(vacationRequestService.getVacationAlerts());
    }

    /** Gerenciador do RH: sem employeeId lista tudo (opcionalmente filtrado por status); com employeeId, filtra por funcionário. */
    @GetMapping
    @PreAuthorize("hasAuthority('rh/vacation-requests:CONSULTAR')")
    public ResponseEntity<List<VacationRequestResponseDTO>> list(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) VacationRequestStatus status
    ) {
        if (employeeId != null) {
            return ResponseEntity.ok(vacationRequestService.listByEmployee(employeeId));
        }
        return ResponseEntity.ok(vacationRequestService.listAll(status));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('rh/vacation-requests:INCLUIR')")
    public ResponseEntity<VacationRequestResponseDTO> registerByRh(
            @Valid @RequestBody CreateVacationByRhDTO request, Authentication auth){
        return ResponseEntity.ok(vacationRequestService.createByRh(request, auth.getName()));
    }
}

