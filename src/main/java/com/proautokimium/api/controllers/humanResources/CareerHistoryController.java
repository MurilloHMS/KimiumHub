package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.CareerHistory.CareerHistoryResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.CareerHistory.CreateCareerHistoryDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.CareerHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/career-histories")
public class CareerHistoryController {

    private final CareerHistoryService careerHistoryService;

    public CareerHistoryController(CareerHistoryService careerHistoryService) {
        this.careerHistoryService = careerHistoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<List<CareerHistoryResponseDTO>> listByEmployee(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(careerHistoryService.listByEmployee(employeeId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<CareerHistoryResponseDTO> create(@Valid @RequestBody CreateCareerHistoryDTO dto) {
        var created = careerHistoryService.create(dto);
        return ResponseEntity.created(URI.create("/api/hr/career-histories?employeeId=" + dto.employeeId()))
                .body(created);
    }
}
