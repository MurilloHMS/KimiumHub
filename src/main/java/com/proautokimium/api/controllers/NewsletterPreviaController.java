package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.newsletter.*;
import com.proautokimium.api.Infrastructure.services.newsletter.NewsletterPreviaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * A revisão da newsletter, antes de qualquer e-mail sair.
 *
 * Quatro endpoints e um fluxo só: busca a prévia, corrige o que não deu para
 * ler, preenche os e-mails que faltam, confirma.
 */
@RestController
@RequestMapping("/api/newsletter/previa")
public class NewsletterPreviaController {

    private final NewsletterPreviaService service;

    public NewsletterPreviaController(NewsletterPreviaService service) {
        this.service = service;
    }

    /**
     * `POST` e não `GET` porque cria o rascunho quando ele ainda não existe —
     * e criar é o caso comum, não a exceção.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('comunicacao/newsletter-revisao:CONSULTAR')")
    public ResponseEntity<PreviaResponseDTO> buscar(@RequestBody @Valid PreviaRequestDTO dto) {
        return ResponseEntity.ok(service.buscarOuCriar(dto.mes(), dto.ano()));
    }

    @PutMapping("/{previaId}/os/{numeroOs}")
    @PreAuthorize("hasAuthority('comunicacao/newsletter-revisao:ALTERAR')")
    public ResponseEntity<ClienteDaNewsletterDTO> corrigirHora(
            @PathVariable UUID previaId,
            @PathVariable int numeroOs,
            @RequestBody @Valid CorrecaoDeHoraDTO dto) {
        return ResponseEntity.ok(service.corrigirHora(previaId, numeroOs, dto));
    }

    @PutMapping("/{previaId}/emails")
    @PreAuthorize("hasAuthority('comunicacao/newsletter-revisao:ALTERAR')")
    public ResponseEntity<List<ClienteDaNewsletterDTO>> preencherEmails(
            @PathVariable UUID previaId,
            @RequestBody @Valid PreencherEmailsDTO dto) {
        return ResponseEntity.ok(service.preencherEmails(previaId, dto.emails()));
    }

    /**
     * Permissão própria (`ENVIAR`): confirmar é o passo que enche a fila de
     * e-mail. Quem confere os números não é necessariamente quem decide mandar.
     */
    @PostMapping("/{previaId}/confirmar")
    @PreAuthorize("hasAuthority('comunicacao/newsletter-revisao:ENVIAR')")
    public ResponseEntity<Void> confirmar(@PathVariable UUID previaId) {
        service.confirmar(previaId);
        return ResponseEntity.noContent().build();
    }
}
