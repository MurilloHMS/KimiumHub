package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementReportService;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O comprovante de reembolsos para a diretoria.
 *
 * Controller próprio, e não mais um método no ReimbursementController: três
 * fatias de teste carregam aquele, e cada dependência nova lá seria um mock a
 * mais em cada uma.
 *
 * {@code BAIXAR} porque o documento obtém o que já existe (é a mesma regra do
 * relatório de abastecimento). Parâmetros opcionais de propósito: a falta de
 * um vira 400 com mensagem do serviço, e não o erro genérico do Spring.
 */
@RestController
@RequestMapping("/api/hr/reimbursements/report")
public class ReimbursementReportController {

    private final ReimbursementReportService service;

    public ReimbursementReportController(ReimbursementReportService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('rh/reimbursements:BAIXAR')")
    @Operation(summary = "Comprovante de reembolsos",
            description = "PDF por período (data da despesa) e status; com employeeId, anexa os comprovantes")
    public ResponseEntity<byte[]> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "status", required = false) List<ReimbursementStatus> statuses,
            @RequestParam(required = false) UUID employeeId,
            Authentication auth) {
        byte[] pdf = service.generate(from, to, statuses, employeeId, auth.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ReimbursementReportService.fileName(from, to) + "\"")
                .body(pdf);
    }
}
