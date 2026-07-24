package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.PayReimbursementDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReviewReimbursementDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementService;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/reimbursements")
@Tag(name = "Reembolsos", description = "Solicitação e gestão de reembolsos")
public class ReimbursementController {

    private final ReimbursementService service;

    public ReimbursementController(ReimbursementService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Solicita reembolso", description = "Funcionário solicita reembolso com comprovante")
    public ResponseEntity<ReimbursementResponseDTO> request(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
            @RequestParam BigDecimal amount,
            @RequestParam String category,
            @RequestParam String reason,
            @RequestParam("receipt") MultipartFile receipt,
            Authentication auth
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.request(auth.getName(), expenseDate, amount, category, reason, receipt));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<ReimbursementResponseDTO> approve(@PathVariable UUID id, @Valid @RequestBody ReviewReimbursementDTO request, Authentication auth) {
        return ResponseEntity.ok(service.approve(id, request, auth.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<ReimbursementResponseDTO> reject(@PathVariable UUID id, @Valid @RequestBody ReviewReimbursementDTO request, Authentication auth) {
        return ResponseEntity.ok(service.reject(id, request, auth.getName()));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<ReimbursementResponseDTO> pay(@PathVariable UUID id, @Valid @RequestBody PayReimbursementDTO request) {
        return ResponseEntity.ok(service.pay(id, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Meus reembolsos", description = "Lista os reembolsos do funcionário autenticado")
    public ResponseEntity<List<ReimbursementResponseDTO>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth.getName()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<List<ReimbursementResponseDTO>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(service.listByEmployee(employeeId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Gerenciador de reembolsos", description = "Lista todos os reembolsos, opcionalmente filtrados por status")
    public ResponseEntity<List<ReimbursementResponseDTO>> listAll(@RequestParam(required = false) ReimbursementStatus status) {
        return ResponseEntity.ok(service.listAll(status));
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Baixa comprovante", description = "Download do comprovante (dono ou RH/ADMIN)")
    public ResponseEntity<byte[]> receipt(@PathVariable UUID id, Authentication auth) throws IOException {
        Optional<Reimbursement> reimbursementOpt = service.buscar(id);
        if (reimbursementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isRh = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("RH"));

        if (!service.podeAcessar(reimbursementOpt.get(), auth.getName(), isRh)) {
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = service.lerComprovante(reimbursementOpt.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reimbursementOpt.get().getReceiptOriginalFilename() + "\"")
                .body(bytes);
    }
}
