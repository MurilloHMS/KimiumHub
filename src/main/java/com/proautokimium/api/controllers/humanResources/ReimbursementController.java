package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.PayReimbursementDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementSummaryDTO;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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

    @PreAuthorize("hasAuthority('documentos/rh/reimbursements:INCLUIR')")
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
    @PreAuthorize("hasAuthority('rh/reimbursements:ALTERAR')")
    public ResponseEntity<ReimbursementResponseDTO> approve(@PathVariable UUID id, @Valid @RequestBody ReviewReimbursementDTO request, Authentication auth) {
        return ResponseEntity.ok(service.approve(id, request, auth.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('rh/reimbursements:ALTERAR')")
    public ResponseEntity<ReimbursementResponseDTO> reject(@PathVariable UUID id, @Valid @RequestBody ReviewReimbursementDTO request, Authentication auth) {
        return ResponseEntity.ok(service.reject(id, request, auth.getName()));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('rh/reimbursements:CONFIGURAR')")
    public ResponseEntity<ReimbursementResponseDTO> pay(@PathVariable UUID id, @Valid @RequestBody PayReimbursementDTO request) {
        return ResponseEntity.ok(service.pay(id, request));
    }

    @PreAuthorize("hasAuthority('documentos/rh/reimbursements:CONSULTAR')")
    @GetMapping("/me")
    @Operation(summary = "Meus reembolsos", description = "Lista os reembolsos do funcionário autenticado")
    public ResponseEntity<List<ReimbursementResponseDTO>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth.getName()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('rh/reimbursements:CONSULTAR')")
    public ResponseEntity<List<ReimbursementResponseDTO>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(service.listByEmployee(employeeId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('rh/reimbursements:CONSULTAR')")
    @Operation(summary = "Gerenciador de reembolsos", description = "Lista todos os reembolsos, opcionalmente filtrados por status")
    public ResponseEntity<List<ReimbursementResponseDTO>> listAll(
            @RequestParam(required = false) ReimbursementStatus status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(service.listAll(status, month));
    }

    /** Totais do mês para o RH, pela data da despesa. Sem mês, o corrente. */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('rh/reimbursements:CONSULTAR')")
    @Operation(summary = "Totais do mês (RH)", description = "Enviado, pendente, aprovado a pagar e pago, em R$ e quantidade")
    public ResponseEntity<ReimbursementSummaryDTO> summary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(service.summary(month != null ? month : YearMonth.now()));
    }

    /** Os mesmos totais, só do funcionário autenticado. */
    @GetMapping("/me/summary")
    @PreAuthorize("hasAuthority('documentos/rh/reimbursements:CONSULTAR')")
    @Operation(summary = "Meus totais do mês")
    public ResponseEntity<ReimbursementSummaryDTO> mySummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            Authentication auth) {
        return ResponseEntity.ok(service.summaryMine(auth.getName(), month != null ? month : YearMonth.now()));
    }

    /**
     * O dono contesta a recusa: comprovante novo e comentário, uma vez, até 30
     * dias. ALTERAR: muda o estado de um pedido que já existe.
     */
    @PostMapping(value = "/{id}/contest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('documentos/rh/reimbursements:ALTERAR')")
    @Operation(summary = "Contesta a recusa", description = "Comprovante novo e comentário; uma vez, até 30 dias depois da recusa")
    public ResponseEntity<ReimbursementResponseDTO> contest(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            @RequestParam(value = "receipt", required = false) MultipartFile receipt,
            Authentication auth) throws IOException {
        return ResponseEntity.ok(service.contest(id, auth.getName(), comment, receipt));
    }

    @PreAuthorize("hasAnyAuthority('rh/reimbursements:BAIXAR', 'documentos/rh/reimbursements:BAIXAR')")
    @GetMapping("/{id}/receipt")
    @Operation(summary = "Baixa comprovante", description = "Download do comprovante (dono ou RH/ADMIN)")
    public ResponseEntity<byte[]> receipt(@PathVariable UUID id,
                                          @RequestParam(defaultValue = "false") boolean original,
                                          Authentication auth) throws IOException {
        Optional<Reimbursement> reimbursementOpt = service.buscar(id);
        if (reimbursementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // "Vê de todos" é ter a tela do RH — a mesma authority do @PreAuthorize.
        // equals, e não contains: contains("ADMIN") casava com ROLE_ADMINISTRATIVO.
        boolean isRh = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("rh/reimbursements:BAIXAR"));

        if (!service.podeAcessar(reimbursementOpt.get(), auth.getName(), isRh)) {
            throw new AccessDeniedException("Você só pode baixar os seus próprios comprovantes.");
        }

        // `original=true`: o comprovante de antes da contestação, que a primeira análise viu.
        Reimbursement r = reimbursementOpt.get();
        byte[] bytes = original ? service.lerComprovanteOriginal(r) : service.lerComprovante(r);
        String fileName = original ? r.getOriginalReceiptFilename() : r.getReceiptOriginalFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(bytes);
    }
}
