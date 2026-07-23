package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.CollectiveBargainingAdjustment.ApplyCollectiveBargainingAdjustmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.CollectiveBargainingAdjustment.CollectiveBargainingAdjustmentResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.CollectiveBargainingAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/collective-bargaining-adjustments")
public class CollectiveBargainingAdjustmentController {

    private final CollectiveBargainingAdjustmentService service;

    public CollectiveBargainingAdjustmentController(CollectiveBargainingAdjustmentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<CollectiveBargainingAdjustmentResponseDTO> apply(
            @Valid @RequestBody ApplyCollectiveBargainingAdjustmentRequestDTO request) {
        return ResponseEntity.ok(service.apply(request));
    }
}
