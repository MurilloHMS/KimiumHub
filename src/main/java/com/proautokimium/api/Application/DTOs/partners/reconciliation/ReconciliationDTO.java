package com.proautokimium.api.Application.DTOs.partners.reconciliation;

import java.util.List;

/**
 * O que a conciliação com o Sankhya encontrou.
 *
 * <p>Os nomes dizem a <b>ação</b>, e não a categoria: quem marca uma linha na
 * tela está decidindo o que vai acontecer com ela, e é isso que o campo precisa
 * responder.
 *
 * <p><b>Uma linha aparece num balde só.</b> Quando o cliente está inativo no ERP
 * <i>e</i> tem campo diferente, {@code toDeactivate} ganha — é a ação mais
 * consequente, e a linha continua carregando as {@code differences} para o
 * quadro todo ficar visível. Sem essa precedência escrita, qual balde vale
 * dependeria da ordem dos {@code if}, e ninguém saberia qual.
 *
 * @param toCreate     estão no ERP e não aqui — entram como cliente
 * @param toUpdate     estão nos dois, com pelo menos um campo diferente
 * @param toDeactivate ativos aqui e inativos no ERP; nada é apagado
 * @param unchanged    quantos já estão iguais. <b>Contagem, não lista:</b>
 *                     ninguém age sobre linha que não mudou, e listá-las
 *                     esconderia as que mudaram — foram 1734 contra 61 na
 *                     medição
 */
public record ReconciliationDTO(
        List<ReconciliationRowDTO> toCreate,
        List<ReconciliationRowDTO> toUpdate,
        List<ReconciliationRowDTO> toDeactivate,
        int unchanged
) { }
