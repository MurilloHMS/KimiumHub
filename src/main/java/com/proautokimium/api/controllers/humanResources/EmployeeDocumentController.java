package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentUpdateDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeDocumentService;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.enums.humanResources.EmployeeDocumentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/employee-documents")
@Tag(name = "Documentos do Funcionário", description = "Documentos vinculados pelo RH, com tipo e vencimento")
public class EmployeeDocumentController {

    /** Quem tem esta authority vê o documento de qualquer funcionário. */
    private static final String HR_DOWNLOAD = "rh/employee-documents:BAIXAR";

    private final EmployeeDocumentService service;

    public EmployeeDocumentController(EmployeeDocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('rh/employee-documents:INCLUIR')")
    @Operation(summary = "Vincula documento", description = "RH vincula um documento a um funcionário; pode substituir um anterior")
    public ResponseEntity<EmployeeDocumentResponseDTO> link(
            @RequestParam UUID employeeId,
            @RequestParam UUID typeId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) UUID replacesId,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.link(employeeId, typeId, title, dueDate, replacesId, file, auth.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('rh/employee-documents:CONSULTAR')")
    @Operation(summary = "Lista para o RH", description = "Filtros opcionais: funcionário, tipo e situação")
    public ResponseEntity<List<EmployeeDocumentResponseDTO>> search(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID typeId,
            @RequestParam(required = false) EmployeeDocumentStatus status
    ) {
        return ResponseEntity.ok(service.search(employeeId, typeId, status));
    }

    @PreAuthorize("hasAuthority('documentos/rh/documents:CONSULTAR')")
    @GetMapping("/me")
    @Operation(summary = "Meus documentos", description = "Lista os documentos do funcionário autenticado")
    public ResponseEntity<List<EmployeeDocumentResponseDTO>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/employee-documents:ALTERAR')")
    @Operation(summary = "Corrige título, tipo e vencimento")
    public ResponseEntity<EmployeeDocumentResponseDTO> update(@PathVariable UUID id,
                                                              @Valid @RequestBody EmployeeDocumentUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/employee-documents:EXCLUIR')")
    @Operation(summary = "Exclui o documento e o arquivo")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('rh/employee-documents:BAIXAR', 'documentos/rh/documents:BAIXAR')")
    @GetMapping("/{id}/arquivo")
    @Operation(summary = "Baixa documento", description = "Download do documento (dono ou RH)")
    public ResponseEntity<byte[]> file(@PathVariable UUID id, Authentication auth) throws IOException {
        Optional<EmployeeDocument> found = service.find(id);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EmployeeDocument document = found.get();

        // equals, e não contains: contains("ADMIN") casava com ROLE_ADMINISTRATIVO.
        boolean isHr = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(HR_DOWNLOAD));

        // 404 e não 403 para quem não é dono: 403 confirmaria que o id existe.
        if (!service.canAccess(document, auth.getName(), isHr)) {
            return ResponseEntity.notFound().build();
        }

        String filename = document.getOriginalFilename() == null ? "documento" : document.getOriginalFilename();
        MediaType type = document.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(document.getContentType());

        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(service.readFile(document));
    }
}
