package com.proautokimium.api.Application.DTOs.prostock.machine;

/**
 * O que o acerto fez.
 *
 * Devolvido para a tela poder dizer o que aconteceu em vez de só recarregar:
 * "criei 35 programações" e "ajustei o estoque de 10 para 17" são resultados
 * diferentes, e quem clicou precisa saber qual dos dois foi.
 */
public record AlignResultDTO(
        String systemCode,
        String name,
        int stockBefore,
        int scheduledBefore,
        /** Quantas programações nasceram. Zero quando o desvio foi do outro lado. */
        int created,
        /** O estoque depois — muda só quando foi ele que estava atrás. */
        int stockAfter
) {}
