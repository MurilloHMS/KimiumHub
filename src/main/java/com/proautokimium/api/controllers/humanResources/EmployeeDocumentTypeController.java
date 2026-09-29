package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentTypeDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentTypeRequestDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeDocumentTypeService;
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
@RequestMapping("/api/hr/employee-document-types")
@Tag(name = "Tipos de documento do funcionário", description = "ASO, NR, contrato… e os seus avisos de vencimento")
public class EmployeeDocumentTypeController {

    private final EmployeeDocumentTypeService service;

    public EmployeeDocumentTypeController(EmployeeDocumentTypeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('rh/employee-documents:CONSULTAR')")
    @Operation(summary = "Lista os tipos", description = "Ativos e inativos, por nome")
    public ResponseEntity<List<EmployeeDocumentTypeDTO>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/employee-documents:CONFIGURAR')")
    @Operation(summary = "Cria um tipo")
    public ResponseEntity<EmployeeDocumentTypeDTO> create(@Valid @RequestBody EmployeeDocumentTypeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/employee-documents:CONFIGURAR')")
    @Operation(summary = "Altera um tipo e os seus avisos")
    public ResponseEntity<EmployeeDocumentTypeDTO> update(@PathVariable UUID id,
                                                          @Valid @RequestBody EmployeeDocumentTypeRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/employee-documents:CONFIGURAR')")
    @Operation(summary = "Desativa um tipo", description = "Não apaga: documentos vinculados continuam com ele")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
