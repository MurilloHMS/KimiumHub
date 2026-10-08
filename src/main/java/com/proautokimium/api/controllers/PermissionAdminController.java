package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.permission.PermissionDTOs.*;
import com.proautokimium.api.Infrastructure.services.permission.PermissionAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * As telas que configuram quem pode o quê.
 *
 * **Este controller nasce anotado, ao contrário dos outros 215.** Eles esperam
 * o passo 5; este não pode esperar nenhum minuto: um endpoint que grava
 * permissão e aceita qualquer funcionário logado é o buraco maior que existe
 * neste sistema — quem alcança `PUT /users/{id}/grid` se dá tudo com um `curl`,
 * e o front escondendo o menu não muda isso.
 *
 * As authorities eram as duas telas da V87. Desde a V124 (2026-10-08) tudo
 * mora na tela de administração: usuários, acesso e modelos ficam numa tela só,
 * e a migration deu `settings/admin` a quem tinha qualquer uma das duas — sem
 * isso, quem configurava permissões ficaria trancado fora da tela que as
 * configura.
 *
 * Os verbos: CONSULTAR lê, ALTERAR grava a grade (de pessoa ou de modelo),
 * INCLUIR cria modelo, CONFIGURAR mexe em várias pessoas de uma vez (aplicar,
 * desfazer, copiar, reaplicar).
 */
@RestController
@RequestMapping("api/permissions")
public class PermissionAdminController {

    private static final String ADMIN = "settings/admin";

    private final PermissionAdminService service;

    public PermissionAdminController(PermissionAdminService service) {
        this.service = service;
    }

    // ─── Catálogo ────────────────────────────────────────────────────────────

    /** As telas do catálogo, cada uma com as ações que usa: as linhas da grade. */
    @GetMapping("/screens")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<List<ScreenDTO>> screens() {
        return ResponseEntity.ok(service.screens());
    }

    /** Quem acessa cada tela: a aba Telas. Só lê. */
    @GetMapping("/screen-access")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<ScreenAccessOverviewDTO> screenAccess() {
        return ResponseEntity.ok(service.screenAccess());
    }

    // ─── Modelos ─────────────────────────────────────────────────────────────

    /** A lista de modelos. A aba Modelos e o "aplicar modelo" a leem. */
    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<List<TemplateSummaryDTO>> templates() {
        return ResponseEntity.ok(service.templates());
    }

    @GetMapping("/templates/{templateId}/grid")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<TemplateGridDTO> templateGrid(@PathVariable UUID templateId) {
        return ResponseEntity.ok(service.templateGrid(templateId));
    }

    /** A quem este modelo já foi aplicado: o aviso "3 pessoas já receberam". */
    @GetMapping("/templates/{templateId}/applied-to")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<List<UserSummaryDTO>> appliedTo(@PathVariable UUID templateId) {
        return ResponseEntity.ok(service.appliedTo(templateId));
    }

    /** Criar. Com `copyFromId` preenchido, é o duplicar. */
    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('" + ADMIN + ":INCLUIR')")
    public ResponseEntity<TemplateSummaryDTO> create(@RequestBody TemplateFormDTO form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(form));
    }

    @PatchMapping("/templates/{templateId}")
    @PreAuthorize("hasAuthority('" + ADMIN + ":ALTERAR')")
    public ResponseEntity<Void> edit(@PathVariable UUID templateId,
                                     @RequestBody TemplateEditDTO form) {
        service.edit(templateId, form);
        return ResponseEntity.noContent().build();
    }

    /**
     * Grava a grade inteira do modelo.
     *
     * `PUT` e não `PATCH` porque o corpo é a grade completa: ausente é negado.
     * Isso torna o pedido idempotente e elimina a pergunta "e as células que
     * você não mandou?".
     */
    @PutMapping("/templates/{templateId}/grid")
    @PreAuthorize("hasAuthority('" + ADMIN + ":ALTERAR')")
    public ResponseEntity<ApplyResultDTO> saveTemplateGrid(@PathVariable UUID templateId,
                                                           @RequestBody GridDTO grid) {
        int alteradas = service.saveTemplateGrid(templateId, grid);
        return ResponseEntity.ok(new ApplyResultDTO(0, alteradas));
    }

    /**
     * O que o "Reaplicar" faria, pessoa por pessoa. Só lê: CONSULTAR basta.
     */
    @GetMapping("/templates/{templateId}/reapply-preview")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<ReapplyPreviewDTO> reapplyPreview(@PathVariable UUID templateId) {
        return ResponseEntity.ok(service.reapplyPreview(templateId));
    }

    /**
     * Leva a versão nova do modelo a quem já o recebeu, refazendo cada pessoa
     * pela soma dos modelos dela. CONFIGURAR, como o aplicar: alcança várias
     * pessoas e apaga ajuste individual.
     */
    @PostMapping("/templates/{templateId}/reapply")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONFIGURAR')")
    public ResponseEntity<ApplyResultDTO> reapply(@PathVariable UUID templateId, Authentication auth) {
        return ResponseEntity.ok(service.reapply(templateId, auth.getName()));
    }

    // ─── Pessoas ─────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<List<UserSummaryDTO>> users() {
        return ResponseEntity.ok(service.users());
    }

    @GetMapping("/users/{userId}/grid")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONSULTAR')")
    public ResponseEntity<UserGridDTO> userGrid(@PathVariable String userId) {
        return ResponseEntity.ok(service.userGrid(userId));
    }

    @PutMapping("/users/{userId}/grid")
    @PreAuthorize("hasAuthority('" + ADMIN + ":ALTERAR')")
    public ResponseEntity<ApplyResultDTO> saveUserGrid(@PathVariable String userId,
                                                       @RequestBody GridDTO grid) {
        int alteradas = service.saveUserGrid(userId, grid);
        return ResponseEntity.ok(new ApplyResultDTO(1, alteradas));
    }

    /**
     * Aplica um modelo a N pessoas.
     *
     * Exige `CONFIGURAR` e não `ALTERAR`: alterar é mexer numa pessoa, e isto
     * alcança várias de uma vez — inclusive apagando ajuste individual quando o
     * modo é SUBSTITUIR. São dois pesos diferentes.
     */
    @PostMapping("/templates/{templateId}/apply")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONFIGURAR')")
    public ResponseEntity<ApplyResultDTO> apply(@PathVariable UUID templateId,
                                                @RequestBody ApplyTemplateDTO form,
                                                Authentication auth) {
        return ResponseEntity.ok(service.apply(templateId, form, auth.getName()));
    }

    /**
     * Desfaz a aplicação de um modelo numa pessoa.
     *
     * `DELETE` sobre o registro da aplicação, e não sobre o modelo: o que se
     * apaga é o fato de ele ter sido copiado nesta pessoa — junto com as
     * permissões que **só** ele deu. O modelo continua existindo, e quem mais o
     * recebeu continua com ele.
     *
     * Exige `CONFIGURAR` pelo mesmo motivo do aplicar: tira acesso de alguém.
     */
    @DeleteMapping("/users/{userId}/templates/{templateId}")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONFIGURAR')")
    public ResponseEntity<ApplyResultDTO> undoApply(@PathVariable String userId,
                                                    @PathVariable UUID templateId) {
        return ResponseEntity.ok(service.undoApply(userId, templateId));
    }

    @PostMapping("/users/{userId}/copy-from/{sourceUserId}")
    @PreAuthorize("hasAuthority('" + ADMIN + ":CONFIGURAR')")
    public ResponseEntity<ApplyResultDTO> copyFrom(@PathVariable String userId,
                                                   @PathVariable String sourceUserId) {
        int alteradas = service.copyFrom(userId, sourceUserId);
        return ResponseEntity.ok(new ApplyResultDTO(1, alteradas));
    }
}
