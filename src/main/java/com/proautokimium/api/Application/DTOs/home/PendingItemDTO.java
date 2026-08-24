package com.proautokimium.api.Application.DTOs.home;

import com.proautokimium.api.domain.enums.home.PendingType;

import java.time.LocalDateTime;

/**
 * Uma linha da home.
 *
 * `since` é quando a pendência nasceu, não quando vence: serve para ordenar da
 * mais antiga esquecida para a mais recente, que é a ordem em que elas
 * incomodam.
 */
public record PendingItemDTO(
        PendingType type,
        String title,
        String detail,
        LocalDateTime since
) {}
