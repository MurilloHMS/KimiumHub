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
 * As authorities são as duas telas da V87. Sem elas, ninguém configura nada —
 * e é por isso que a V87 as abre para o ADMIN direto no banco: não existe saída
 * pela interface quando a interface é justamente o que está trancado.
 */
@RestController
@RequestMapping("api/permissions")
public class PermissionAdminController {

    private static final String TEMPLATES = "settings/permissions/templates";
    private static final String USERS = "settings/permissions/users";

    private final PermissionAdminService service;

    public PermissionAdminController(PermissionAdminService service) {
        this.service = service;
    }

    // ─── Catálogo ────────────────────────────────────────────────────────────

    /**
     * As telas do catálogo — as linhas da grade.
     *
     * Serve às duas telas, então basta **qualquer uma** das duas permissões:
     * exigir a de modelos para desenhar a grade de um usuário trancaria quem só
     * cuida de pessoas.
     */
    @GetMapping("/screens")
    @PreAuthorize("hasAnyAuthority('" + TEMPLATES + ":CONSULTAR', '" + USERS + ":CONSULTAR')")
    public ResponseEntity<List<ScreenDTO>> screens() {
        return ResponseEntity.ok(service.screens());
    }

    // ─── Modelos ─────────────────────────────────────────────────────────────

    /** A lista de modelos. Também serve às duas telas: o "aplicar modelo" a lê. */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('" + TEMPLATES + ":CONSULTAR', '" + USERS + ":CONSULTAR')")
    public ResponseEntity<List<TemplateSummaryDTO>> templates() {
        return ResponseEntity.ok(service.templates());
    }

    @GetMapping("/templates/{templateId}/grid")
    @PreAuthorize("hasAuthority('" + TEMPLATES + ":CONSULTAR')")
    public ResponseEntity<TemplateGridDTO> templateGrid(@PathVariable UUID templateId) {
        return ResponseEntity.ok(service.templateGrid(templateId));
    }

    /**
     * A quem este modelo já foi aplicado.
     *
     * Abre com qualquer uma das duas permissões: é a lista que sustenta o aviso
     * "aplicado a 3 pessoas" na tela de modelos.
     */
    @GetMapping("/templates/{templateId}/applied-to")
    @PreAuthorize("hasAnyAuthority('" + TEMPLATES + ":CONSULTAR', '" + USERS + ":CONSULTAR')")
    public ResponseEntity<List<UserSummaryDTO>> appliedTo(@PathVariable UUID templateId) {
        return ResponseEntity.ok(service.appliedTo(templateId));
    }

    /** Criar. Com `copyFromId` preenchido, é o duplicar. */
    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('" + TEMPLATES + ":INCLUIR')")
    public ResponseEntity<TemplateSummaryDTO> create(@RequestBody TemplateFormDTO form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(form));
    }

    @PatchMapping("/templates/{templateId}")
    @PreAuthorize("hasAuthority('" + TEMPLATES + ":ALTERAR')")
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
    @PreAuthorize("hasAuthority('" + TEMPLATES + ":ALTERAR')")
    public ResponseEntity<ApplyResultDTO> saveTemplateGrid(@PathVariable UUID templateId,
                                                           @RequestBody GridDTO grid) {
        int alteradas = service.saveTemplateGrid(templateId, grid);
        return ResponseEntity.ok(new ApplyResultDTO(0, alteradas));
    }

    // ─── Pessoas ─────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('" + USERS + ":CONSULTAR')")
    public ResponseEntity<List<UserSummaryDTO>> users() {
        return ResponseEntity.ok(service.users());
    }

    @GetMapping("/users/{userId}/grid")
    @PreAuthorize("hasAuthority('" + USERS + ":CONSULTAR')")
    public ResponseEntity<UserGridDTO> userGrid(@PathVariable String userId) {
        return ResponseEntity.ok(service.userGrid(userId));
    }

    @PutMapping("/users/{userId}/grid")
    @PreAuthorize("hasAuthority('" + USERS + ":ALTERAR')")
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
    @PreAuthorize("hasAuthority('" + USERS + ":CONFIGURAR')")
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
    @PreAuthorize("hasAuthority('" + USERS + ":CONFIGURAR')")
    public ResponseEntity<ApplyResultDTO> undoApply(@PathVariable String userId,
                                                    @PathVariable UUID templateId) {
        return ResponseEntity.ok(service.undoApply(userId, templateId));
    }

    @PostMapping("/users/{userId}/copy-from/{sourceUserId}")
    @PreAuthorize("hasAuthority('" + USERS + ":CONFIGURAR')")
    public ResponseEntity<ApplyResultDTO> copyFrom(@PathVariable String userId,
                                                   @PathVariable String sourceUserId) {
        int alteradas = service.copyFrom(userId, sourceUserId);
        return ResponseEntity.ok(new ApplyResultDTO(1, alteradas));
    }
}
