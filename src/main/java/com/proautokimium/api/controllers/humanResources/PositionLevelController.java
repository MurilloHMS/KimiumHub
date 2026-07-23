package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.PositionLevel.CreatePositionLevelRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.PositionLevel.PositionLevelResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.PositionLevelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/position-levels")
public class PositionLevelController {

    private final PositionLevelService positionLevelService;

    public PositionLevelController(PositionLevelService positionLevelService) {
        this.positionLevelService = positionLevelService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<PositionLevelResponseDTO> create(@Valid @RequestBody CreatePositionLevelRequestDTO request) {
        return ResponseEntity.ok(positionLevelService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<List<PositionLevelResponseDTO>> listByPosition(@RequestParam UUID positionId) {
        return ResponseEntity.ok(positionLevelService.listByPosition(positionId));
    }
}
