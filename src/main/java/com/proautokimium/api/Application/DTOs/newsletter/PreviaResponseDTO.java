package com.proautokimium.api.Application.DTOs.newsletter;

import java.util.List;
import java.util.UUID;

/**
 * A prévia inteira.
 *
 * `nomeDoMes` vem daqui, e não do navegador: o mês por extenso aparece na
 * newsletter e na tela, e sair dos dois lugares diferentes é como eles passam a
 * discordar.
 */
public record PreviaResponseDTO(
        UUID id,
        int mes,
        int ano,
        String nomeDoMes,
        List<ClienteDaNewsletterDTO> clientes,
        List<PendenciaDeHoraDTO> pendencias
) {
}
