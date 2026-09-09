package com.proautokimium.api.Application.DTOs.partners.reconciliation;

import jakarta.validation.constraints.NotBlank;

/**
 * Uma linha que a pessoa marcou na tela.
 *
 * <p><b>Diz quem, não o quê.</b> O cliente manda o código autorizado e a
 * assinatura do que viu; quem decide se aquilo é criar, atualizar ou desativar é
 * o servidor, a partir da própria releitura do ERP. Se a tela dissesse "criar" e
 * a linha já existisse, obedecer criaria duplicata.
 *
 * <p>A {@code signature} é o que substitui uma tabela de rascunho: no aplicar
 * ela é recalculada e tem que bater. Não batendo, o ERP mudou desde que a pessoa
 * olhou, e gravar seria escrever algo que ela não aprovou.
 */
public record ReconciliationChoiceDTO(
        @NotBlank String code,
        @NotBlank String signature
) { }
