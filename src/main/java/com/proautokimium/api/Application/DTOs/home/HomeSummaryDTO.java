package com.proautokimium.api.Application.DTOs.home;

import java.util.List;

/**
 * O que a home mostra, numa chamada só.
 *
 * Notificacoes ficam de fora de propósito: a contagem chega ao vivo por STOMP
 * no front, e repetir aqui criaria duas verdades que divergem entre um refresh
 * e um empurrão. Avisos também ficam de fora — o mural devolve a lista que a
 * tela precisa exibir, não um número.
 *
 * As listas nunca vêm nulas. Lista vazia é "não há nada"; nulo obrigaria toda
 * tela a checar antes de iterar, e uma delas esqueceria.
 */
public record HomeSummaryDTO(
        List<PendingItemDTO> mine,
        List<PendingItemDTO> approvals,
        Integer vacationBalanceDays
) {}
