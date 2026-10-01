package com.proautokimium.api.Application.DTOs.home;

import com.proautokimium.api.domain.enums.home.PendingType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma linha da home.
 *
 * `since` é quando a pendência nasceu, não quando vence: serve para ordenar da
 * mais antiga esquecida para a mais recente, que é a ordem em que elas
 * incomodam.
 *
 * `refId` é o registro de que a pendência fala, quando a tela precisa abrir
 * aquele um (o convite do evento). A rota continua sendo assunto do front.
 */
public record PendingItemDTO(
        PendingType type,
        String title,
        String detail,
        LocalDateTime since,
        UUID refId
) {
    public PendingItemDTO(PendingType type, String title, String detail, LocalDateTime since) {
        this(type, title, detail, since, null);
    }
}
