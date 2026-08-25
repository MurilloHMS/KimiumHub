package com.proautokimium.api.Application.DTOs.prostock.machine;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma alteração de previsão, como a tela lê. */
public record ScheduleChangeDTO(
        UUID id,
        LocalDateTime previsaoAnterior,
        LocalDateTime previsaoNova,
        String motivo,
        String changedBy,
        LocalDateTime changedAt
) {}
