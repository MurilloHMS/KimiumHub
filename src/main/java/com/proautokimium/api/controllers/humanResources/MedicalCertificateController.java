package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.EmployeeMedicalCertificatesDTO;
import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.MedicalCertificateResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.MedicalCertificateService;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificate;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/medical-certificates")
@Tag(name = "Atestados", description = "Envio de atestados médicos e histórico")
public class MedicalCertificateController {

    private final MedicalCertificateService service;

    public MedicalCertificateController(MedicalCertificateService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia atestado", description = "Funcionário envia atestado por foto ou arquivo")
    public ResponseEntity<MedicalCertificateResponseDTO> submit(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam SubmissionType submissionType,
            @RequestParam(required = false) Boolean confirmedLegible,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.submit(auth.getName(), startDate, endDate, submissionType, confirmedLegible, file));
    }

    @GetMapping("/me")
    @Operation(summary = "Meus atestados", description = "Histórico do funcionário autenticado")
    public ResponseEntity<List<MedicalCertificateResponseDTO>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth.getName()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Histórico do funcionário", description = "Histórico completo + contagem de atestados no ano corrente")
    public ResponseEntity<EmployeeMedicalCertificatesDTO> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(service.getForRh(employeeId));
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Baixa atestado", description = "Download do atestado (dono ou RH/ADMIN)")
    public ResponseEntity<byte[]> file(@PathVariable UUID id, Authentication auth) throws IOException {
        Optional<MedicalCertificate> certificateOpt = service.buscar(id);
        if (certificateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isRh = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("RH"));

        if (!service.podeAcessar(certificateOpt.get(), auth.getName(), isRh)) {
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = service.lerArquivo(certificateOpt.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + certificateOpt.get().getOriginalFilename() + "\"")
                .body(bytes);
    }
}
