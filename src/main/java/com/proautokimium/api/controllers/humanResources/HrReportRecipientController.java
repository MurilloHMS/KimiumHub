package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.ReportRecipient.AddHrReportRecipientDTO;
import com.proautokimium.api.Application.DTOs.humanResources.ReportRecipient.HrReportRecipientDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.HrReportRecipientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Quem recebe os relatórios do RH por e-mail.
 *
 * Mora na tela de Reembolsos, sem tela nova: ler é CONSULTAR, e mudar a lista
 * é CONFIGURAR — decidir para onde vão nome e valor de todo mundo pesa como
 * pagar, não como aprovar.
 */
@RestController
@RequestMapping("/api/hr/report-recipients")
public class HrReportRecipientController {

    private final HrReportRecipientService service;

    public HrReportRecipientController(HrReportRecipientService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('rh/reimbursements:CONSULTAR')")
    @Operation(summary = "Destinatários dos relatórios do RH")
    public ResponseEntity<List<HrReportRecipientDTO>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/reimbursements:CONFIGURAR')")
    @Operation(summary = "Adiciona um destinatário")
    public ResponseEntity<HrReportRecipientDTO> add(@Valid @RequestBody AddHrReportRecipientDTO dto,
                                                    Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(dto.email(), auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/reimbursements:CONFIGURAR')")
    @Operation(summary = "Remove um destinatário")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }
}
