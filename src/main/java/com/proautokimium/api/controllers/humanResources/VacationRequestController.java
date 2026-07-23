package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.ReviewVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.VacationRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping
    public ResponseEntity<VacationRequestResponseDTO> create(@Valid @RequestBody CreateVacationRequestDTO request) {
        return ResponseEntity.ok(vacationRequestService.create(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<VacationRequestResponseDTO> approve(
            @PathVariable UUID id, @Valid @RequestBody ReviewVacationRequestDTO request) {
        return ResponseEntity.ok(vacationRequestService.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<VacationRequestResponseDTO> reject(
            @PathVariable UUID id, @Valid @RequestBody ReviewVacationRequestDTO request) {
        return ResponseEntity.ok(vacationRequestService.reject(id, request));
    }

    @GetMapping
    public ResponseEntity<List<VacationRequestResponseDTO>> listByEmployee(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(vacationRequestService.listByEmployee(employeeId));
    }
}
