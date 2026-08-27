package com.proautokimium.api.Application.DTOs.permission;

import com.proautokimium.api.domain.enums.ApplyMode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O vocabulário das telas de configuração de permissão.
 *
 * Num arquivo só, e não em onze: são records de uma linha que só existem
 * juntos — quem lê `TemplateGridDTO` quer ver `GridDTO` na mesma tela, e
 * espalhar isso por onze arquivos faz alguém abrir onze abas para entender uma
 * requisição.
 */
public final class PermissionDTOs {

    private PermissionDTOs() { }

    /** Uma tela do catálogo, do jeito que o grid a desenha. */
    public record ScreenDTO(String code, String label, String module, int sortOrder) { }

    /** Uma linha da lista lateral de modelos. */
    public record TemplateSummaryDTO(UUID id, String name, String description,
                                     boolean active, long allowedCells, long stampedUsers) { }

    /** Um modelo aberto, com a grade inteira. */
    public record TemplateGridDTO(UUID id, String name, String description,
                                  boolean active, Map<String, List<String>> cells) { }

    /** Uma linha da lista lateral de usuários. */
    public record UserSummaryDTO(String id, String name, String login,
                                 boolean active, List<String> templates) { }

    /** Um carimbo que já passou por alguém. */
    public record AppliedTemplateDTO(UUID id, String name, OffsetDateTime appliedAt,
                                     String appliedBy, ApplyMode mode) { }

    /**
     * A grade de uma pessoa.
     *
     * `stamped` é o que os modelos carimbados nela permitem — a comparação com
     * `cells` é o ponto âmbar da tela. É derivado, não guardado: `user_templates`
     * já registra por onde a pessoa passou, e uma coluna a mais em
     * `user_permissions` só repetiria isso com risco de discordar.
     *
     * O que ele **não** distingue: uma célula que divergiu porque alguém a
     * ajustou, e uma que divergiu porque o modelo mudou depois. Por isso a tela
     * diz "difere dos carimbos aplicados", e não "ajuste individual".
     */
    public record UserGridDTO(String id, String name, String login,
                              Map<String, List<String>> cells,
                              Map<String, List<String>> stamped,
                              List<AppliedTemplateDTO> appliedTemplates) { }

    /**
     * A grade inteira, na ida.
     *
     * **Ausente é negado.** Manda-se o que está ligado, e o `PUT` grava a grade
     * completa — o que torna o pedido idempotente e elimina a pergunta "e as
     * células que você não mandou?". São ~55 arrays curtos no lugar de 385
     * objetos.
     */
    public record GridDTO(Map<String, List<String>> cells) { }

    /** Criar ou duplicar: `copyFromId` preenchido é o duplicar. */
    public record TemplateFormDTO(String name, String description, UUID copyFromId) { }

    /** Renomear ou desativar. */
    public record TemplateEditDTO(String name, String description, Boolean active) { }

    /** Carimbar um modelo em N pessoas. */
    public record ApplyTemplateDTO(List<String> userIds, ApplyMode mode) { }

    /** O que o carimbo mexeu, para a tela poder dizer em vez de só fechar. */
    public record ApplyResultDTO(int users, int cellsChanged) { }
}
