package com.proautokimium.api.controllers.dev;

import com.proautokimium.api.Application.DTOs.email.EmailQueueDTOs.*;
import com.proautokimium.api.Infrastructure.services.email.EmailSenderAdminService;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** A tela "Remetentes": os e-mails da empresa e de qual deles sai cada serviço. */
@RestController
@RequestMapping("/api/dev/email-senders")
@Tag(name = "Remetentes de e-mail", description = "E-mails da empresa e rotas por origem")
public class EmailSenderAdminController {

    private final EmailSenderAdminService service;

    public EmailSenderAdminController(EmailSenderAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('dev/email-senders:CONSULTAR')")
    public ResponseEntity<List<Sender>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('dev/email-senders:INCLUIR')")
    @Operation(summary = "Cadastra remetente", description = "Endereço = nome + @envios.proautokimium.com.br")
    public ResponseEntity<Sender> create(@RequestBody CreateSender body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('dev/email-senders:ALTERAR')")
    @Operation(summary = "Muda nome de exibição ou ativa/desativa", description = "Em uso ou padrão não se desativa")
    public ResponseEntity<Sender> update(@PathVariable UUID id, @RequestBody UpdateSender body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAuthority('dev/email-senders:ALTERAR')")
    public ResponseEntity<Sender> makeDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(service.makeDefault(id));
    }

    @GetMapping("/routes")
    @PreAuthorize("hasAuthority('dev/email-senders:CONSULTAR')")
    @Operation(summary = "Quem envia o quê", description = "Uma linha por origem; senderId nulo = remetente padrão")
    public ResponseEntity<List<Route>> routes() {
        return ResponseEntity.ok(service.listRoutes());
    }

    @PutMapping("/routes/{origin}")
    @PreAuthorize("hasAuthority('dev/email-senders:ALTERAR')")
    public ResponseEntity<Route> updateRoute(@PathVariable EmailOrigin origin, @RequestBody UpdateRoute body,
                                             Authentication auth) {
        return ResponseEntity.ok(service.updateRoute(origin, body, auth.getName()));
    }
}
