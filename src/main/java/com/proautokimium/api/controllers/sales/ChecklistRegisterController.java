package com.proautokimium.api.controllers.sales;

import com.proautokimium.api.Application.DTOs.sales.ChecklistRegisterDTO;
import com.proautokimium.api.Infrastructure.services.sales.ChecklistRegisterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Os cadastros do checklist, mantidos pela Controladoria ({@code vendas/checklist-cadastros}). */
@RestController
@RequestMapping("/api/checklists/registers")
public class ChecklistRegisterController {

    private final ChecklistRegisterService service;

    public ChecklistRegisterController(ChecklistRegisterService service) {
        this.service = service;
    }

    @GetMapping("/visual-items")
    @PreAuthorize("hasAuthority('vendas/checklist-cadastros:CONSULTAR')")
    public ResponseEntity<List<ChecklistRegisterDTO.VisualItem>> visualItems() {
        return ResponseEntity.ok(service.visualItems());
    }

    @PostMapping("/visual-items")
    @PreAuthorize("hasAuthority('vendas/checklist-cadastros:INCLUIR')")
    public ResponseEntity<ChecklistRegisterDTO.VisualItem> createVisualItem(
            @Valid @RequestBody ChecklistRegisterDTO.VisualItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVisualItem(request));
    }

    @PutMapping("/visual-items/{id}")
    @PreAuthorize("hasAuthority('vendas/checklist-cadastros:ALTERAR')")
    public ResponseEntity<ChecklistRegisterDTO.VisualItem> updateVisualItem(
            @PathVariable UUID id, @Valid @RequestBody ChecklistRegisterDTO.VisualItemRequest request) {
        return ResponseEntity.ok(service.updateVisualItem(id, request));
    }

    @GetMapping("/comodato-items")
    @PreAuthorize("hasAuthority('vendas/checklist-cadastros:CONSULTAR')")
    public ResponseEntity<List<ChecklistRegisterDTO.ComodatoItem>> comodatoItems() {
        return ResponseEntity.ok(service.comodatoItems());
    }

    @GetMapping("/comodato-candidates")
    @PreAuthorize("hasAuthority('vendas/checklist-cadastros:CONSULTAR')")
    public ResponseEntity<List<ChecklistRegisterDTO.ComodatoCandidate>> comodatoCandidates() {
        return ResponseEntity.ok(service.comodatoCandidates());
    }

    @PostMapping("/comodato-items")
    @PreAuthorize("hasAuthority('vendas/checklist-cadastros:INCLUIR')")
    public ResponseEntity<ChecklistRegisterDTO.ComodatoItem> createComodatoItem(
            @Valid @RequestBody ChecklistRegisterDTO.ComodatoItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createComodatoItem(request));
    }

    @PutMapping("/comodato-items/{id}")
    @PreAuthorize("hasAuthority('vendas/checklist-cadastros:ALTERAR')")
    public ResponseEntity<ChecklistRegisterDTO.ComodatoItem> updateComodatoItem(
            @PathVariable UUID id, @Valid @RequestBody ChecklistRegisterDTO.ComodatoItemRequest request) {
        return ResponseEntity.ok(service.updateComodatoItem(id, request));
    }
}
