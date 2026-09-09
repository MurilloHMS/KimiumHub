package com.proautokimium.api.Application.DTOs.newsletter;

/**
 * Uma OS cuja hora não deu para interpretar.
 *
 * Vai com o **texto original** de propósito: é o que permite decidir. Sem ele a
 * tela seria um formulário sem pergunta.
 */
public record PendenciaDeHoraDTO(
        int numeroOs,
        String codigoCliente,
        String nomeDoCliente,
        String horaInicio,
        String horaFim
) {
}
