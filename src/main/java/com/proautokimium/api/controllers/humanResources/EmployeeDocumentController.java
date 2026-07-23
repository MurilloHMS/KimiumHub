package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeDocumentService;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/employee-documents")
@Tag(name = "Documentos do Funcionário", description = "Documentos assinados vinculados pelo RH")
public class EmployeeDocumentController {

    private final EmployeeDocumentService service;

    public EmployeeDocumentController(EmployeeDocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Vincula documento", description = "RH vincula um documento assinado a um funcionário")
    public ResponseEntity<EmployeeDocumentResponseDTO> vincular(
            @RequestParam UUID employeeId,
            @RequestParam String title,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.vincular(employeeId, title, file));
    }

    @GetMapping("/me")
    @Operation(summary = "Meus documentos", description = "Lista os documentos do funcionário autenticado")
    public ResponseEntity<List<EmployeeDocumentResponseDTO>> meus(Authentication auth) {
        return ResponseEntity.ok(service.listarDoFuncionario(auth.getName()));
    }

    @GetMapping("/{id}/arquivo")
    @Operation(summary = "Baixa documento", description = "Download do documento (dono ou RH/ADMIN)")
    public ResponseEntity<byte[]> arquivo(@PathVariable UUID id, Authentication auth) throws IOException {
        Optional<EmployeeDocument> docOpt = service.buscar(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isRh = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("RH"));

        if (!service.podeAcessar(docOpt.get(), auth.getName(), isRh)) {
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = service.lerArquivo(docOpt.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + docOpt.get().getOriginalFilename() + "\"")
                .body(bytes);
    }
}
