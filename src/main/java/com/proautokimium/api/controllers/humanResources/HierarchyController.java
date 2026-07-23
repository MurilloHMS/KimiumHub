package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.CreateHierarchyRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.HierarchyResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.HierarchyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/hierarchies")
public class HierarchyController {

    private final HierarchyService hierarchyService;

    public HierarchyController(HierarchyService hierarchyService) {
        this.hierarchyService = hierarchyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<HierarchyResponseDTO> create(@Valid @RequestBody CreateHierarchyRequestDTO request) {
        return ResponseEntity.ok(hierarchyService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<List<HierarchyResponseDTO>> listAll() {
        return ResponseEntity.ok(hierarchyService.listAll());
    }
}
