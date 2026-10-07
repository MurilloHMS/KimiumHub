package com.proautokimium.api.controllers.dev;

import com.proautokimium.api.Application.DTOs.email.EmailQueueDTOs.*;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueAdminService;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** A tela "Fila de e-mails" do desenvolvedor: ver o que saiu, o que falhou e por quê, e reenviar. */
@RestController
@RequestMapping("/api/dev/email-queue")
@Tag(name = "Fila de e-mails", description = "Acompanhamento e reenvio dos e-mails do ERP")
public class EmailQueueAdminController {

    private final EmailQueueAdminService service;

    public EmailQueueAdminController(EmailQueueAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('dev/email-queue:CONSULTAR')")
    @Operation(summary = "Lista a fila", description = "status (ou QUEUE), origem, período em dias (1, 7, 30) e busca")
    public ResponseEntity<EmailPage> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) EmailOrigin origin,
                                          @RequestParam(required = false) Integer days,
                                          @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate since,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.list(status, origin, days, since, q, page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dev/email-queue:CONSULTAR')")
    @Operation(summary = "Indicadores do período")
    public ResponseEntity<Summary> summary(@RequestParam(required = false) Integer days,
                                           @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate since) {
        return ResponseEntity.ok(service.summary(days, since));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dev/email-queue:CONSULTAR')")
    @Operation(summary = "Um e-mail", description = "O corpo de e-mail de acesso (código ou link) vem escondido")
    public ResponseEntity<EmailDetail> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(service.detail(id));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAuthority('dev/email-queue:ALTERAR')")
    @Operation(summary = "Reenvia", description = "Só o que falhou volta para a fila, com as tentativas zeradas")
    public ResponseEntity<EmailRow> resend(@PathVariable UUID id) {
        return ResponseEntity.ok(service.resend(id));
    }

    @PostMapping("/resend")
    @PreAuthorize("hasAuthority('dev/email-queue:ALTERAR')")
    @Operation(summary = "Reenvia em lote", description = "Ignora os que não falharam")
    public ResponseEntity<ResendResult> resend(@RequestBody ResendRequest body) {
        return ResponseEntity.ok(service.resend(body.ids()));
    }
}
