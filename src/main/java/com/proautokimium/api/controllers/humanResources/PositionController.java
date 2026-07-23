package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Position.CreatePositionRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Position.PositionResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.PositionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<PositionResponseDTO> create(@Valid @RequestBody CreatePositionRequestDTO request) {
        return ResponseEntity.ok(positionService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<List<PositionResponseDTO>> listAll() {
        return ResponseEntity.ok(positionService.listAll());
    }
}
